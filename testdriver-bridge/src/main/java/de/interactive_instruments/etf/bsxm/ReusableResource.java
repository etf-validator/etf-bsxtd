/**
 * Copyright 2017-2020 European Union, interactive instruments GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * This work was supported by the EU Interoperability Solutions for
 * European Public Administrations Programme (http://ec.europa.eu/isa)
 * through Action 1.17: A Reusable INSPIRE Reference Platform (ARE3NA).
 */
package de.interactive_instruments.etf.bsxm;

import java.io.*;

import org.basex.query.QueryException;
import org.basex.query.QueryModule;
import org.basex.query.value.Value;
import org.basex.query.value.item.Item;
import org.basex.query.value.item.Str;
import org.basex.query.value.map.XQMap;

import de.interactive_instruments.IFile;

public class ReusableResource extends QueryModule {

    private IFile storeDir;

    public ReusableResource() throws QueryException {

    }

    public void init(final String attachmentDir) {
        this.storeDir = new IFile(attachmentDir);
    }

    private IFile getFile(String name) {
        return storeDir.secureExpandPathDown(name + ".obj");
    }

    @Requires(Permission.ADMIN)
    public boolean existsObj(final Object obj) {
        return getFile(obj.getClass().getName()).exists();
    }

    @Requires(Permission.ADMIN)
    public boolean existsObjByName(final String name) {
        return getFile(name).exists();
    }

    @Requires(Permission.ADMIN)
    public void store(final Object obj) throws QueryException {
        store(obj, obj.getClass().getName());
    }

    @Requires(Permission.ADMIN)
    public void store(final Object obj, final String name) throws QueryException {
        if (!(obj instanceof Externalizable)) {
            throw new QueryException("The reusable resource does not implement the externalizable interface: " +
                    obj.getClass().getName());
        }
        try (final FileOutputStream fileOutputStream = new FileOutputStream(getFile(name));
                final ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            ((Externalizable) obj).writeExternal(objectOutputStream);
        } catch (IOException e) {
            throw new QueryException("Failed to write reusable resource: " + e.getMessage());
        }
    }

    @Deterministic
    @Requires(Permission.ADMIN)
    public void restore(final Object obj) throws QueryException {
        restore(obj, obj.getClass().getName());
    }

    @Deterministic
    @Requires(Permission.ADMIN)
    public void restore(final Object obj, final String name) throws QueryException {
        try (final FileInputStream fileInputStream = new FileInputStream(getFile(name));
                final ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            ((Externalizable) obj).readExternal(objectInputStream);
        } catch (IOException | ClassNotFoundException e) {
            throw new QueryException("Failed to read reusable resource: " + e.getMessage());
        }
    }

    @Deterministic
    @Requires(Permission.NONE)
    public XQMap restoreMap(final String name) throws QueryException {
        XQMap map = XQMap.EMPTY;
        try (final FileInputStream fileInputStream = new FileInputStream(getFile(name));
                final ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            final long size = objectInputStream.readLong();
            for (long i = 0; i < size; i++) {
                final ExternalizedMapEntry entry = (ExternalizedMapEntry) objectInputStream.readObject();
                map = map.put(Str.get(entry.getKey()), entry.getValues(this.queryContext), null);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new QueryException("Failed to restore reusable resource " + name + " : " + e.getMessage());
        }
        return map;
    }

    @Requires(Permission.NONE)
    public void storeMap(final XQMap map, final String name) throws QueryException {
        try (final FileOutputStream fileOutputStream = new FileOutputStream(getFile(name));
                final ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeLong(map.keys().size());
            for (final Item key : map.keys()) {
                final Value value = map.get(key, null);
                objectOutputStream.writeObject(new ExternalizedMapEntry(key, value));
            }
        } catch (IOException e) {
            getFile(name).delete();
            throw new QueryException("Failed to store reusable resource " + name + " : " + e.getMessage());
        }
    }
}
