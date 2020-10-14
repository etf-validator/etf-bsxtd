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

import java.io.Externalizable;

import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.value.Value;
import org.basex.query.value.ValueBuilder;
import org.basex.query.value.item.Item;
import org.basex.query.value.seq.Seq;
import org.basex.query.value.type.AtomType;
import org.basex.query.value.type.NodeType;
import org.basex.util.InputInfo;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public interface BigArray extends Externalizable {

    int ARRAY_MAX_SIZE = Integer.MAX_VALUE - 5;

    BigArray add(final Value value);

    Seq sequence(final QueryContext qc);

    long size();

    Item get(final QueryContext qc, final long index);

    class EntrySequence extends Seq {

        private final BigArray arr;
        private final QueryContext qc;

        public EntrySequence(final QueryContext qc, final BigArray arr) {
            super(arr.size(), NodeType.NOD);
            this.arr = arr;
            this.qc = qc;
        }

        @Override
        public Value atomValue(final QueryContext qc, final InputInfo ii) throws QueryException {
            final ValueBuilder vb = new ValueBuilder(qc);
            for (int i = 0; i < size; i++)
                vb.add(itemAt(i).atomValue(qc, ii));
            return vb.value(AtomType.AAT);
        }

        @Override
        public Item ebv(final QueryContext qc, final InputInfo ii) throws QueryException {
            return itemAt(0);
        }

        @Override
        public Value insert(final long pos, final Item value, final QueryContext qc) {
            throw new UnsupportedOperationException("Reusable Resource: insert not supported");
        }

        @Override
        public Value remove(final long pos, final QueryContext qc) {
            throw new UnsupportedOperationException("Reusable Resource: remove not supported");
        }

        @Override
        public void cache(final boolean lazy, final InputInfo ii) throws QueryException {

        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof EntrySequence))
                return super.equals(obj);
            final EntrySequence es = (EntrySequence) obj;
            return size == es.size && arr.equals(es.arr);
        }

        @Override
        public boolean ddo() {
            return true;
        }

        @Override
        public long atomSize() {
            return arr.size();
        }

        @Override
        public Item itemAt(final long pos) {
            return arr.get(qc, pos);
        }

        @Override
        public Value reverse(final QueryContext qc) {
            throw new UnsupportedOperationException("Reusable Resource: reverse not supported");
        }
    }
}
