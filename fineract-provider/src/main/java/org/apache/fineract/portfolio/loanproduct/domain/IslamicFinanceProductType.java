/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.loanproduct.domain;

import java.util.HashMap;
import java.util.Map;

public enum IslamicFinanceProductType {

    QARD_HASSAN(1, "islamicFinanceProductType.qard.hassan"), //
    MURABAHA(2, "islamicFinanceProductType.murabaha"); //

    private final Integer value;
    private final String code;

    private static final Map<Integer, IslamicFinanceProductType> intToEnumMap = new HashMap<>();

    static {
        for (final IslamicFinanceProductType type : IslamicFinanceProductType.values()) {
            intToEnumMap.put(type.value, type);
        }
    }

    public static IslamicFinanceProductType fromInt(final Integer ruleTypeValue) {
        final IslamicFinanceProductType type = intToEnumMap.get(ruleTypeValue);
        return type;
    }

    IslamicFinanceProductType(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    @Override
    public String toString() {
        return name().toString().replaceAll("_", " ");
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }
}
