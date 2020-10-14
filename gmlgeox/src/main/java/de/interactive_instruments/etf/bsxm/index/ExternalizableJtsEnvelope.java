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
package de.interactive_instruments.etf.bsxm.index;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.vividsolutions.jts.geom.Envelope;

/**
 * ExternalizableJtsEnvelope
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class ExternalizableJtsEnvelope implements Externalizable {
    private double minx;
    private double maxx;
    private double miny;
    private double maxy;

    public ExternalizableJtsEnvelope() {}

    ExternalizableJtsEnvelope(final Envelope envelope) {
        this.minx = envelope.getMinX();
        this.maxx = envelope.getMaxX();
        this.miny = envelope.getMinY();
        this.maxy = envelope.getMaxY();
    }

    public Envelope toEnvelope() {
        return new Envelope(this.minx, this.maxx, this.miny, this.maxy);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeDouble(this.minx);
        out.writeDouble(this.maxx);
        out.writeDouble(this.miny);
        out.writeDouble(this.maxy);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        this.minx = in.readDouble();
        this.maxx = in.readDouble();
        this.miny = in.readDouble();
        this.maxy = in.readDouble();
    }
}
