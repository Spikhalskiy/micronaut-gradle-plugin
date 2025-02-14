/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.gradle.openapi.tasks;

import io.micronaut.gradle.openapi.ParameterMappingModel;
import io.micronaut.gradle.openapi.ResponseBodyMappingModel;
import io.micronaut.openapi.generator.AbstractMicronautJavaCodegen;
import io.micronaut.openapi.generator.MicronautCodeGeneratorBuilder;
import io.micronaut.openapi.generator.MicronautCodeGeneratorEntryPoint;
import io.micronaut.openapi.generator.MicronautCodeGeneratorOptionsBuilder;
import io.micronaut.openapi.generator.SerializationLibraryKind;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.util.Locale;

public abstract class AbstractOpenApiWorkAction<T extends AbstractOpenApiWorkAction.OpenApiParameters> implements WorkAction<T> {
    interface OpenApiParameters extends WorkParameters {
        RegularFileProperty getDefinitionFile();

        Property<String> getInvokerPackageName();

        Property<String> getApiPackageName();

        Property<String> getModelPackageName();

        Property<Boolean> getUseBeanValidation();

        Property<Boolean> getUseOptional();

        Property<Boolean> getUseReactive();

        ListProperty<String> getOutputKinds();

        Property<String> getSerializationFramework();

        DirectoryProperty getOutputDirectory();

        Property<Boolean> getAlwaysUseGenerateHttpResponse();

        Property<Boolean> getGenerateHttpResponseWhereRequired();

        Property<String> getDateTimeFormat();

        ListProperty<ParameterMappingModel> getParameterMappings();

        ListProperty<ResponseBodyMappingModel> getResponseBodyMappings();
    }

    protected abstract void configureBuilder(MicronautCodeGeneratorBuilder builder);

    @Override
    public void execute() {
        var parameters = getParameters();
        var builder = MicronautCodeGeneratorEntryPoint.builder()
                .withDefinitionFile(parameters.getDefinitionFile().get().getAsFile().toURI())
                .withOutputDirectory(parameters.getOutputDirectory().getAsFile().get())
                .withOutputs(
                        parameters.getOutputKinds().get()
                                .stream()
                                .map(s -> MicronautCodeGeneratorEntryPoint.OutputKind.valueOf(s.toUpperCase(Locale.US)))
                                .toArray(MicronautCodeGeneratorEntryPoint.OutputKind[]::new)
                )
                .withOptions(options -> {
                    options.withInvokerPackage(parameters.getInvokerPackageName().get());
                    options.withApiPackage(parameters.getApiPackageName().get());
                    options.withModelPackage(parameters.getModelPackageName().get());
                    options.withBeanValidation(parameters.getUseBeanValidation().get());
                    options.withOptional(parameters.getUseOptional().get());
                    options.withReactive(parameters.getUseReactive().get());
                    options.withSerializationLibrary(SerializationLibraryKind.valueOf(parameters.getSerializationFramework().get().toUpperCase(Locale.US)));
                    options.withGenerateHttpResponseAlways(parameters.getAlwaysUseGenerateHttpResponse().get());
                    options.withGenerateHttpResponseWhereRequired(parameters.getGenerateHttpResponseWhereRequired().get());
                    options.withDateTimeFormat(MicronautCodeGeneratorOptionsBuilder.DateTimeFormat.valueOf(parameters.getDateTimeFormat().get().toUpperCase(Locale.US)));
                    options.withParameterMappings(parameters.getParameterMappings()
                            .get()
                            .stream()
                            .map(mapping -> new AbstractMicronautJavaCodegen.ParameterMapping(
                                    mapping.getName(),
                                    AbstractMicronautJavaCodegen.ParameterMapping.ParameterLocation.valueOf(mapping.getLocation().name()),
                                    mapping.getMappedType(),
                                    mapping.getMappedName(),
                                    mapping.isValidated())
                            )
                            .toList()
                    );
                    options.withResponseBodyMappings(parameters.getResponseBodyMappings()
                            .get()
                            .stream()
                            .map(mapping -> new AbstractMicronautJavaCodegen.ResponseBodyMapping(mapping.getHeaderName(), mapping.getMappedBodyType(), mapping.isListWrapper(), mapping.isValidated()))
                            .toList()
                    );
                });
        configureBuilder(builder);
        builder.build().generate();
    }
}
