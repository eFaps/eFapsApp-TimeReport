/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
package org.efaps.esjp.timereport;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIFormTimeReport;
import org.efaps.esjp.ci.CITimeReport;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("ef181682-1f77-4948-a0bc-69e08fe892fd")
@EFapsApplication("eFapsApp-TimeReport")
public abstract class EmployeePreplannedAbsence_Base
    extends CommonDocument
{

    /**
     * Create a EmployeeReport.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        ;
        final CreatedDoc createdDoc = createDoc(_parameter);
        connect2Object(_parameter, createdDoc);
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    protected CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc ret = new CreatedDoc();

        final Create create = new Create()
        {

            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                throws EFapsException
            {
                super.add2basicInsert(_parameter, _insert);
                add2createDoc(_parameter, _insert, ret);
            }
        };

        final Instance docInst = create.basicInsert(_parameter);
        ret.setInstance(docInst);
        create.connect(_parameter, docInst);

        return ret;
    }

    protected void add2createDoc(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        addStatus2DocCreate(_parameter, _insert, _createdDoc);
        _insert.add(CITimeReport.EmployeePreplannedAbsence.Name, getDocName4Create(_parameter));
        _insert.add(CITimeReport.EmployeePreplannedAbsence.EmployeeLink, Instance.get(_parameter
                        .getParameterValue(CIFormTimeReport.TimeReport_EmployeePreplannedAbsenceForm.employee.name)));
    }

    public Return createPosition(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create()
        {

            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                throws EFapsException
            {
                super.add2basicInsert(_parameter, _insert);
                final PrintQuery print = new PrintQuery(_parameter.getInstance());
                print.addAttribute(CITimeReport.EmployeePreplannedAbsence.EmployeeLink);
                print.execute();
                _insert.add(CITimeReport.EmployeePreplannedAbsencePosition.EmployeeLink,
                                print.<Long>getAttribute(CITimeReport.EmployeePreplannedAbsence.EmployeeLink));
            }
        };
        return create.execute(_parameter);
    }
}
