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
package de.interactive_instruments.etf.bsxm.geometry;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.List;

import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.geometry.GeometryFactory;
import org.deegree.geometry.linearization.LinearizationCriterion;
import org.deegree.geometry.linearization.MaxErrorCriterion;
import org.deegree.geometry.primitive.Curve;
import org.deegree.geometry.primitive.Ring;
import org.deegree.geometry.primitive.segments.CurveSegment;

/** Based on the implementation of {@link GeometryFactory}. */
public class IIGeometryFactory extends GeometryFactory implements Externalizable {

    protected double maxError = 0.00001;
    protected int maxNumPoints = 1000;

    @Override
    public Curve createCurve(final String id, final ICRS crs, final CurveSegment... segments) {
        return (Curve) inspect(new IICurve(id, crs, pm, Arrays.asList(segments), this));
    }

    @Override
    public Ring createRing(final String id, final ICRS crs, final List<Curve> members) {
        return (Ring) inspect(new IIRing(id, crs, pm, members, this));
    }

    /**
     * @param maxError
     *            the maxError to set
     */
    public void setMaxError(double maxError) {
        this.maxError = maxError;
    }

    /**
     * @param maxNumPoints
     *            the maxNumPoints to set
     */
    public void setMaxNumPoints(int maxNumPoints) {
        this.maxNumPoints = maxNumPoints;
    }

    public LinearizationCriterion getMaxErrorCriterion() {

        return new MaxErrorCriterion(maxError, maxNumPoints);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        /* Thus far we are not concerned with fields from supertypes - the default values of these fields suffice. */

        out.writeDouble(maxError);
        out.writeInt(maxNumPoints);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        /* Thus far we are not concerned with fields from supertypes - the default values of these fields suffice. */

        this.maxError = in.readDouble();
        this.maxNumPoints = in.readInt();
    }
}
