/*
 * Copyright 2003 - 2015 The eFaps Team
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

package org.efaps.esjp.timereport.listener;

import java.math.BigDecimal;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CITimeReport;
import org.efaps.esjp.payroll.listener.IOnAdvance;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("46c3ca20-6be4-43ef-8433-4bf815050e56")
@EFapsRevision("$Rev: 14133 $")
public abstract class IOnAdvance_Base
    extends AbstractOnPayroll
    implements IOnAdvance
{

    @Override
    public BigDecimal getLaborTime(final Parameter _parameter,
                                   final Instance _advanceInst,
                                   final DateTime _date,
                                   final Instance _emplInst)
        throws EFapsException
    {
        setDate(_date);
        setEmplInst(_emplInst);
        final TimeBean bean = getTimeBean(_parameter);
        return bean.getLaborTime();
    }

    @Override
    public BigDecimal getExtraLaborTime(final Parameter _parameter,
                                        final Instance _advanceInst,
                                        final DateTime _date,
                                        final Instance _emplInst)
        throws EFapsException
    {
        setDate(_date);
        setEmplInst(_emplInst);
        final TimeBean bean = getTimeBean(_parameter);
        return bean.getExtraLaborTime();
    }

    @Override
    public BigDecimal getNightLaborTime(final Parameter _parameter,
                                        final Instance _advanceInst,
                                        final DateTime _date,
                                        final Instance _emplInst)
        throws EFapsException
    {
        setDate(_date);
        setEmplInst(_emplInst);
        final TimeBean bean = getTimeBean(_parameter);
        return bean.getNightLaborTime();
    }

    @Override
    public BigDecimal getHolidayLaborTime(final Parameter _parameter,
                                          final Instance _advanceInst,
                                          final DateTime _date,
                                          final Instance _emplInst)
        throws EFapsException
    {
        setDate(_date);
        setEmplInst(_emplInst);
        final TimeBean bean = getTimeBean(_parameter);
        return bean.getHolidayLaborTime();
    }

    @Override
    public int getWeight()
    {
        return 0;
    }

    @Override
    protected void add2QueryBldr(final Parameter _parameter,
                                 final QueryBuilder _queryBldr) throws EFapsException
    {
        _queryBldr.addWhereAttrEqValue(CITimeReport.EmployeeTimeCardPosition.EmployeeLink, getEmplInst());
        _queryBldr.addWhereAttrLessValue(CITimeReport.EmployeeTimeCardPosition.Date, getDate().plusDays(1));
    }
}
