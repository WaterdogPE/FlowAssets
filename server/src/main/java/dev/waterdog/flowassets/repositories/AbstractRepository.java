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

package dev.waterdog.flowassets.repositories;

import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.function.ValueProvider;
import dev.waterdog.flowassets.utils.DataProviderBuilder;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class AbstractRepository<T> implements PanacheRepository<T> {

    public abstract String nameIdentifier();

    @Transactional
    public List<T> findByName(String name) {
        return this.find(this.nameIdentifier() + " like ?1", "%" + name.trim() + "%").list();
    }

    @Transactional
    public long countByName(String name) {
        return this.count(this.nameIdentifier() + " like ?1", "%" + name.trim() + "%");
    }

    @Transactional
    public List<T> findByName(String name, int pageIndex, int pageSize) {
        return this.find(this.nameIdentifier() + " like ?1", "%" + name.trim() + "%").page(pageIndex, pageSize).list();
    }

    @Transactional
    public T getByName(String name) {
        return this.find(this.nameIdentifier(), name.trim()).firstResult();
    }

    @Transactional
    public List<T> getAll() {
        return this.listAll();
    }

    @Transactional
    public List<T> getAll(int pageIndex, int pageSize) {
        return this.listAll().subList(pageIndex, pageSize);
    }

    @Transactional
    public long count() {
        return PanacheRepository.super.count();
    }

    @Transactional
    public T save(T value) {
        if (!this.isPersistent(value)) {
            value = this.getEntityManager().merge(value);
        }

        this.persist(value);
        return value;
    }

    @Transactional
    public void remove(T value) {
        if (this.isPersistent(value)) {
            this.delete(value);
        } else {
            this.delete(this.getEntityManager().merge(value));
        }
    }

    public static <T> DataProvider<T, String> createDataProvider(AbstractRepository<T> repository) {
        return DataProviderBuilder.create(repository)
                .build();
    }

    public static <T> DataProvider<T, String> createDataProviderMapped(AbstractRepository<T> repository, ValueProvider<T, Object> identifierGetter) {
        return DataProviderBuilder.create(repository)
                .identifierGetter(identifierGetter)
                .build();
    }

    public static <T, R> DataProvider<R, String> createDataProvider(AbstractRepository<T> repository, Function<Stream<T>, Stream<R>> mapper) {
        DataProviderBuilder<T, R> builder = DataProviderBuilder.create();
        builder.repository(repository);
        builder.mapper(mapper);
        return builder.build();
    }
}
