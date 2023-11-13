/*
 * Copyright (C) 2023 Juraj Antal
 *
 * Originally created in G-Watch App
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sk.trupici.gwatch.wear.followers;

import androidx.annotation.Nullable;

/**
 * Communication exception representing HTTP 429 status code
 */
public class TooManyRequestsException extends CommunicationException {
    private final String retryAfter;

    public TooManyRequestsException(String retryAfter) {
        this.retryAfter = retryAfter;
    }
    public TooManyRequestsException(String retryAfter, String message) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public TooManyRequestsException(String retryAfter, String message, Throwable cause) {
        super(message, cause);
        this.retryAfter = retryAfter;
    }

    @Nullable
    public String getRetryAfter() {
        return retryAfter;
    }
}
