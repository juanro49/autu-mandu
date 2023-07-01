/*
 * Copyright 2017 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.juanro.autumandu.data.report;

import java.util.Date;

/**
 * The chart libraries supports only float values. In order to fit timestamps in the charts without
 * loosing too much precision we convert the dates like this:
 * 1. Use days since first timestamp.
 * 2. Store time of day in the part after the decimal point.
 * More info: https://bitbucket.org/frigus02/car-report/issues/83
 */
class ReportDateHelper {
    static float toFloat(Date date) {
        return (date.getTime() / 1000L) / 86400.0f;
    }

    static Date toDate(float date) {
        return new Date(((long) (date * 86400.0f)) * 1000L);
    }
}
