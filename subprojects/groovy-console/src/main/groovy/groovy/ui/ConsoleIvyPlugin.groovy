/*
 * Copyright 2003-2012 the original author or authors.
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
package groovy.ui

import groovy.grape.GrapeIvy
import groovy.grape.Grape

import org.apache.ivy.core.event.resolve.StartResolveEvent
import org.apache.ivy.core.event.download.PrepareDownloadEvent
import org.apache.ivy.core.event.IvyListener

/**
 * Groovy Swing console.
 *
 * Adds Groovy Grape feedback (via an Ivy listener).
 *
 * @author Guillaume Laforge
 * @author Paul King
 */
class ConsoleIvyPlugin {
    Console savedConsole
    Set<String> resolvedDependencies = []
    Set<String> downloadedArtifacts = []

    def addListener(Console console) {
        savedConsole = console

        ((GrapeIvy) Grape.instance).ivyInstance.eventManager.addIvyListener([progress: { ivyEvent ->
            switch (ivyEvent) {
                case StartResolveEvent:
                    ivyEvent.moduleDescriptor.dependencies.each { it ->
                        def name = it.toString()
                        if (!resolvedDependencies.contains(name)) {
                            resolvedDependencies << name
                            savedConsole.showMessage "Resolving ${name} ..."
                        }
                    }
                    break
                case PrepareDownloadEvent:
                    ivyEvent.artifacts.each { it ->
                        def name = it.toString()
                        if (!downloadedArtifacts.contains(name)) {
                            downloadedArtifacts << name
                            savedConsole.showMessage "Downloading artifact ${name} ..."
                        }
                    }
                    break
            }
        }] as IvyListener)

    }
}
