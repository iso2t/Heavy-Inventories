"""Release helper commands used by release workflows."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys


class ReleaseError(Exception):
    """Raised when release validation fails."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ReleaseError(message)


def git(*args: str, root: Path) -> str:
    result = subprocess.run(["git", *args], cwd=root, capture_output=True, text=True)
    require(result.returncode == 0, f"Git command failed: {' '.join(args)}")
    return result.stdout.strip()


def release_branches(root: Path) -> list[str]:
    policy = root / "release" / "publishing.json"
    if not policy.is_file():
        return []

    data = json.loads(policy.read_text(encoding="utf-8"))
    branches = data.get("release_branches")
    require(isinstance(branches, list) and branches, "No trusted release branches configured")

    validated: list[str] = []
    for branch in branches:
        require(isinstance(branch, str), "Invalid release branch")
        require(re.fullmatch(r"[A-Za-z0-9_./-]+", branch) and ".." not in branch, "Invalid release branch")
        validated.append(branch)
    return validated


def resolve(root: Path, tag: str) -> str:
    require(tag.startswith("v"), "Release tag must start with v")
    sha = git("rev-parse", "--verify", f"refs/tags/{tag}^{{commit}}", root=root)

    branches = release_branches(root)
    if branches:
        trusted = False
        for branch in branches:
            check = subprocess.run(
                ["git", "merge-base", "--is-ancestor", sha, f"refs/remotes/origin/{branch}"],
                cwd=root,
                capture_output=True,
                text=True,
            )
            if check.returncode == 0:
                trusted = True
            elif check.returncode != 1:
                raise ReleaseError(f"Git merge-base failed for branch {branch}")
        require(trusted, "Tag must point to a commit merged into a configured release branch")

    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as handle:
            handle.write(f"tag={tag}\nsha={sha}\n")

    return sha


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["resolve"])
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--tag", default=os.environ.get("RELEASE_TAG"))
    args = parser.parse_args()

    require(args.command == "resolve", "Unsupported command")
    require(bool(args.tag), "Missing release tag")

    print(resolve(args.root, args.tag))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (ReleaseError, KeyError, ValueError, OSError, json.JSONDecodeError) as error:
        print("Release stopped: " + str(error), file=sys.stderr)
        raise SystemExit(1)
