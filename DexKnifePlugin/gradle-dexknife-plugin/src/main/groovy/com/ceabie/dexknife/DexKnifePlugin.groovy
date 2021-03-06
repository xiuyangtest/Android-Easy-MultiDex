/*
 * Copyright (C) 2016 ceabie (https://github.com/ceabie/)
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
package com.ceabie.dexknife

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * the spilt tools plugin.
 */
public class DexKnifePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        //过滤Activity组件
        project.afterEvaluate {
            File file = project.file(DexSplitTools.DEX_KNIFE_CFG_TXT)
            if (file != null) {
                def justActivitys = [];
                file.eachLine { line ->
                    //printf "read line ${line}\n";
                    if (line.startsWith('-just activity')) {
                        line = line.replaceAll('-just activity', '').trim();
                        justActivitys.add(line)
                    }
                }
                printf "-just activity size = ${justActivitys.size()}\n";
                if (justActivitys.size() != 0) {
                    project.tasks.each { task ->
                        if (task.name.startsWith('collect') && task.name.endsWith('MultiDexComponents')) {
                            println "main-dex-filter: found task $task.name"
                            task.filter { name, attrs ->
                                String componentName = attrs.get('android:name')
                                if ('activity'.equals(name)) {
                                    def result = justActivitys.find {
                                        componentName.endsWith("${it}")
                                    }
                                    def bool = result != null;
                                    if (bool) {
                                        printf "main-dex-filter: keep ${componentName}\n"
                                    }
                                    return bool
                                }
                                return true
                            }
                        }
                    }
                }
            }
        }

        project.afterEvaluate {
            for (variant in project.android.applicationVariants) {
                if (isMultiDexEnabled(variant)) {
                    if (SplitToolsFor130.isCompat(variant)) {
                        System.err.println("DexKnife: Compat 1.3.0.");
                        SplitToolsFor130.processSplitDex(project, variant)
                    } else if (SplitToolsFor150.isCompat()) {
                        SplitToolsFor150.processSplitDex(project, variant)
                    } else {
                        System.err.println("DexKnife Error: DexKnife is not compatible your Android gradle plugin.");
                    }
                } else {
                    System.err.println("DexKnife : MultiDexEnabled is false, it's not work.");
                }
            }
        }
    }

    private static boolean isMultiDexEnabled(variant) {
        def is = variant.buildType.multiDexEnabled
        if (is != null) {
            return is;
        }

        is = variant.mergedFlavor.multiDexEnabled
        if (is != null) {
            return is;
        }

        return false
    }

}