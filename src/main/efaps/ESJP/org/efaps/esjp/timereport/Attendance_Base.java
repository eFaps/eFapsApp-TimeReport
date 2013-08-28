/*
 * Copyright 2003 - 2013 The eFaps Team
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
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.timereport;

import java.util.ArrayList;
import java.util.List;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIFormTimeReport;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CITimeReport;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.common.uiform.Field_Base.ListType;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("5e8e3fe4-a95d-4381-aa47-2684c9d4368e")
@EFapsRevision("$Rev$")
public abstract class Attendance_Base
{

    public Return myAttendanceMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIHumanResource.Employee);
                attrQueryBldr.addWhereAttrEqValue(CIHumanResource.Employee.UserPerson,
                                Context.getThreadContext().getPersonId());
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIHumanResource.Employee.ID);
                _queryBldr.addWhereAttrInQuery(CITimeReport.AttendanceAbstract.EmployeeAbstractLink, attrQuery);
            }
        };
        return multi.execute(_parameter);
    }

    public Return myAttendanceStartStop(final Parameter _parameter)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIHumanResource.Employee);
        queryBldr.addWhereAttrEqValue(CIHumanResource.Employee.UserPerson, Context.getThreadContext().getPersonId());
        final InstanceQuery query = queryBldr.getQuery();
        final List<Instance> empList = query.executeWithoutAccessCheck();
        if (!empList.isEmpty()) {
            final Insert insert = new Insert(Type.get(Long.valueOf(_parameter
                            .getParameterValue(CIFormTimeReport.TimeReport_AttendanceSelfRegStartStopForm.type.name))));
            insert.add(CITimeReport.AttendanceAbstract.Time, _parameter
                            .getParameterValue(CIFormTimeReport.TimeReport_AttendanceSelfRegStartStopForm.time.name));
            insert.add(CITimeReport.AttendanceAbstract.EmployeeAbstractLink, empList.get(0));
            insert.execute();
        }
        return new Return();
    }

    public Return getMyAttandanceRadioList(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Field field = new Field();
        final boolean start = new DateTime().isBefore(new DateTime().withHourOfDay(12));
        final List<DropDownPosition> values = new ArrayList<DropDownPosition>();
        final DropDownPosition pos = new DropDownPosition(CITimeReport.AttendanceSelfRegStart.getType().getId(),
                        CITimeReport.AttendanceSelfRegStart.getType().getLabel());
        values.add(pos);
        pos.setSelected(start);
        final DropDownPosition pos2 = new DropDownPosition(CITimeReport.AttendanceSelfRegEnd.getType().getId(),
                        CITimeReport.AttendanceSelfRegEnd.getType().getLabel());
        values.add(pos2);
        pos2.setSelected(!start);
        ret.put(ReturnValues.SNIPLETT, field.getInputField(_parameter, values, ListType.RADIO));
        return ret;
    }

}
