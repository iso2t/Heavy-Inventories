"""Release helper commands used by release workflows."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys

TAG_PATTERN = re.compile(r"v[0-9]+(?:\.[0-9]+){2,3}(?:-(?:alpha|beta|rc)\.[1-9][0-9]*)?$")


class ReleaseError(Exception):
    """Raised when release validation fails."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ReleaseError(message)


def git(*args: str, root: Path) -> str:
    result = subprocess.run(["git", *args], cwd=root, capture_output=True, text=True)
    error = result.stderr.strip()
    require(result.returncode == 0, f"Git command failed: {' '.join(args)}{f' ({error})' if error else ''}")
    return result.stdout.strip()


def release_branches(root: Path) -> list[str]:
    policy = root / "release" / "publishing.json"
    if not policy.is_file():
        return []

    data = json.loads(policy.read_text(encoding="utf-8"))
    require(isinstance(data, dict), "Invalid release branch configuration")
    if "release_branches" not in data:
        return []
    branches = data.get("release_branches")
    require(isinstance(branches, list), "Invalid release branch configuration")

    validated: list[str] = []
    for branch in branches:
        require(isinstance(branch, str), "Invalid release branch")
        format_check = subprocess.run(
            ["git", "check-ref-format", "--branch", branch],
            cwd=root,
            capture_output=True,
            text=True,
        )
        require(format_check.returncode == 0, "Invalid release branch")
        validated.append(branch)
    return validated


def resolve_branch_ref(root: Path, branch: str) -> str:
    candidates = [f"refs/heads/{branch}", f"refs/remotes/origin/{branch}", f"origin/{branch}", branch]
    for ref in candidates:
        check = subprocess.run(
            ["git", "rev-parse", "--verify", "--quiet", f"{ref}^{{commit}}"],
            cwd=root,
            capture_output=True,
            text=True,
        )
        if check.returncode == 0:
            return ref
    raise ReleaseError(f"Release branch ref not found for {branch}")


def resolve(root: Path, tag: str) -> str:
    require(TAG_PATTERN.fullmatch(tag) is not None, "Release tag must match vX.Y.Z[.W][-alpha.N|-beta.N|-rc.N]")
    sha = git("rev-parse", "--verify", f"refs/tags/{tag}^{{commit}}", root=root)

    branches = release_branches(root)
    if branches:
        trusted = False
        for branch in branches:
            ref = resolve_branch_ref(root, branch)
            check = subprocess.run(
                ["git", "merge-base", "--is-ancestor", sha, ref],
                cwd=root,
                capture_output=True,
                text=True,
            )
            if check.returncode == 0:
                trusted = True
                break
            elif check.returncode != 1:
                raise ReleaseError(f"Git merge-base failed for branch {branch}")
        require(trusted, "Tag must point to a commit merged into a configured release branch")

    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as handle:
            handle.write(f"tag<<__HI__\n{tag}\n__HI__\n")
            handle.write(f"sha<<__HI__\n{sha}\n__HI__\n")

    return sha


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["resolve"])
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--tag", default=os.environ.get("RELEASE_TAG"))
    args = parser.parse_args()

    require(bool(args.tag), "Missing release tag")

    print(resolve(args.root, args.tag))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (ReleaseError, KeyError, ValueError, OSError, json.JSONDecodeError) as error:
        print("Release stopped: " + str(error), file=sys.stderr)
        raise SystemExit(1)
