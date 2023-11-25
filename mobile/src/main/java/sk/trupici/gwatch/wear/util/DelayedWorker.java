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

package sk.trupici.gwatch.wear.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/**
 * Dummy worker to avoid widget flickering on update.
 * <p>
 * Primary cause:
 * The flickering is caused by the current implementation of WorkManager :(
 * <p>
 * The workaround:
 * The current workaround is to create a constrained one-time work request with an initial delay set to very long time.
 * <p>
 * The idea behind:
 * The long delayed schedule ensures that the WorkManager component is not disabled and an app widget update is not triggered recursively.
 */
public class DelayedWorker extends Worker {
    public DelayedWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        return Result.success();
    }

    public static void schedule(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.enqueueUniqueWork(
                "DelayedWorker",
                ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(DelayedWorker.class)
                .setInitialDelay(10 * 365, TimeUnit.DAYS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build()
                ).build());
    }
}
