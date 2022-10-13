/*
 * Copyright 2022 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.flowassets.utils;

import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.function.ValueProvider;
import dev.waterdog.flowassets.repositories.AbstractRepository;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataProviderBuilder<T, R> {
    private AbstractRepository<T> repository;
    private Function<Stream<T>, Stream<R>> mapper;
    private ValueProvider<R, Object> identifierGetter;
    private R[] extraValues;

    public static <T> DataProviderBuilder<T, T> create(AbstractRepository<T> repository) {
        DataProviderBuilder<T, T> builder = new DataProviderBuilder<>();
        builder.repository(repository);
        builder.mapper(val -> val);
        return builder;
    }

    public static <T, R> DataProviderBuilder<T, R> create() {
        return new DataProviderBuilder<>();
    }

    public DataProviderBuilder<T, R> repository(AbstractRepository<T> repository) {
        if (repository == null) {
            throw new NullPointerException("Repository cannot be null");
        }
        this.repository = repository;
        return this;
    }

    public DataProviderBuilder<T, R> mapper(Function<Stream<T>, Stream<R>> mapper) {
        if (mapper == null) {
            throw new NullPointerException("Mapper cannot be null");
        }
        this.mapper = mapper;
        return this;
    }

    public DataProviderBuilder<T, R> identifierGetter(ValueProvider<R, Object> identifierGetter) {
        if (identifierGetter == null) {
            throw new NullPointerException("Itemgetter cannot be null");
        }
        this.identifierGetter = identifierGetter;
        return this;
    }

    public DataProviderBuilder<T, R> extraValues(R... values) {
        this.extraValues = values;
        return this;
    }

    public DataProvider<R, String> build() {
        if (this.repository == null) {
            throw new NullPointerException("Repository was not set");
        }

        if (this.mapper == null) {
            throw new NullPointerException("Mapper was not set");
        }

        ValueProvider<R, Object> identifierSupplier = this.identifierGetter == null ?
                val -> val : this.identifierGetter;


        return new CallbackDataProvider<>(query -> {
            List<T> list;
            if (query.getFilter().isPresent()) {
                list = this.repository.findByName(query.getFilter().get(), query.getPage(), query.getPageSize());
            } else {
                list = this.repository.getAll(query.getPage(), query.getPageSize());
            }

            if (this.extraValues == null || this.extraValues.length == 0) {
                return this.mapper.apply(list.stream());
            }
            return Stream.concat(mapper.apply(list.stream()), Stream.of(extraValues));
        }, query -> {
            int extraCount = this.extraValues == null ? 0 : this.extraValues.length;
            return (int) (query.getFilter().isPresent() ? this.repository.countByName(query.getFilter().get()) : repository.count()) + extraCount;
        }, identifierSupplier);
    }


}
