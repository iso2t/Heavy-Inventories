package com.iso2t.heavyinventories.api.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Functions {

	public static <T> T either (T conditionTrue, T conditionFalse, boolean condition) {
		return condition ? conditionTrue : conditionFalse;
	}

	public static <T> T either (boolean condition, T valueTrue, T valueFalse) {
		return either(valueTrue, valueFalse, condition);
	}

}
