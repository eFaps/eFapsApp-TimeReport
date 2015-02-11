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
import org.efaps.esjp.payroll.listener.IOnPayslip;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("04a65fab-e47f-4349-9d59-6a490dc184c1")
@EFapsRevision("$Rev: 14133 $")
public abstract class OnPayslip_Base
    implements IOnPayslip
{

    @Override
    public BigDecimal getLaborTime(final Parameter _parameter,
                                   final DateTime _date,
                                   final DateTime _dueDate,
                                   final Instance _emplInst)
        throws EFapsException
    {
        return BigDecimal.TEN;
    }

    @Override
    public BigDecimal getExtraLaborTime(final Parameter _parameter,
                                        final DateTime _date,
                                        final DateTime _dueDate,
                                        final Instance _emplInst)
        throws EFapsException
    {
        return BigDecimal.TEN;
    }

    @Override
    public BigDecimal getNightLaborTime(final Parameter _parameter,
                                        final DateTime _date,
                                        final DateTime _dueDate,
                                        final Instance _emplInst)
        throws EFapsException
    {
        return BigDecimal.TEN;
    }

    @Override
    public BigDecimal getHolidayLaborTime(final Parameter _parameter,
                                          final DateTime _date,
                                          final DateTime _dueDate,
                                          final Instance _emplInst)
        throws EFapsException
    {
        return BigDecimal.TEN;
    }

    @Override
    public int getWeight()
    {
        return 0;
    }
}
