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
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CITimeReport;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("be7bb3ea-2e4d-43bb-8dc7-173d18573aca")
@EFapsRevision("$Rev: 14133 $")
public abstract class AbstractOnPayroll_Base
    extends AbstractCommon
{

    private Instance emplInst;

    private DateTime date;

    private DateTime dueDate;

    protected TimeBean getTimeBean(final Parameter _parameter)
        throws EFapsException
    {
        final TimeBean ret = new TimeBean();
        final QueryBuilder queryBldr = new QueryBuilder(CITimeReport.EmployeeTimeCardPosition);
        add2QueryBldr(_parameter, queryBldr);
        final MultiPrintQuery multi = queryBldr.getCachedPrint4Request();
        multi.addAttribute(CITimeReport.EmployeeTimeCardPosition.LaborTime,
                        CITimeReport.EmployeeTimeCardPosition.ExtraLaborTime,
                        CITimeReport.EmployeeTimeCardPosition.NightLaborTime,
                        CITimeReport.EmployeeTimeCardPosition.HolidayLaborTime);
        multi.execute();
        while (multi.next()) {
            ret.addLaborTime(multi.<Object[]>getAttribute(CITimeReport.EmployeeTimeCardPosition.LaborTime));
            ret.addExtraLaborTime(multi.<Object[]>getAttribute(CITimeReport.EmployeeTimeCardPosition.ExtraLaborTime));
            ret.addNightLaborTime(multi.<Object[]>getAttribute(CITimeReport.EmployeeTimeCardPosition.NightLaborTime));
            ret.addHolidayLaborTime(multi
                            .<Object[]>getAttribute(CITimeReport.EmployeeTimeCardPosition.HolidayLaborTime));
        }
        return ret;
    }

    /**
     * @param _parameter
     */
    protected abstract void add2QueryBldr(final Parameter _parameter,
                                          final QueryBuilder _queryBldr)
        throws EFapsException;

    public static class TimeBean
    {

        private BigDecimal laborTime;
        private BigDecimal extraLaborTime;
        private BigDecimal nightLaborTime;
        private BigDecimal holidayLaborTime;

        public void addLaborTime(final Object[] _valueWithUom)
        {
            if (this.laborTime == null) {
                this.laborTime = BigDecimal.ZERO;
            }
            this.laborTime = this.laborTime.add((BigDecimal) _valueWithUom[0]);
        }

        public void addExtraLaborTime(final Object[] _valueWithUom)
        {
            if (this.extraLaborTime == null) {
                this.extraLaborTime = BigDecimal.ZERO;
            }
            this.extraLaborTime = this.extraLaborTime.add((BigDecimal) _valueWithUom[0]);
        }

        public void addNightLaborTime(final Object[] _valueWithUom)
        {
            if (this.nightLaborTime == null) {
                this.nightLaborTime = BigDecimal.ZERO;
            }
            this.nightLaborTime = this.nightLaborTime.add((BigDecimal) _valueWithUom[0]);
        }

        public void addHolidayLaborTime(final Object[] _valueWithUom)
        {
            if (this.holidayLaborTime == null) {
                this.holidayLaborTime = BigDecimal.ZERO;
            }
            this.holidayLaborTime = this.holidayLaborTime.add((BigDecimal) _valueWithUom[0]);
        }

        /**
         * Getter method for the instance variable {@link #laborTime}.
         *
         * @return value of instance variable {@link #laborTime}
         */
        public BigDecimal getLaborTime()
        {
            return this.laborTime;
        }

        /**
         * Setter method for instance variable {@link #laborTime}.
         *
         * @param _laborTime value for instance variable {@link #laborTime}
         */
        public void setLaborTime(final BigDecimal _laborTime)
        {
            this.laborTime = _laborTime;
        }

        /**
         * Getter method for the instance variable {@link #extraLaborTime}.
         *
         * @return value of instance variable {@link #extraLaborTime}
         */
        public BigDecimal getExtraLaborTime()
        {
            return this.extraLaborTime;
        }

        /**
         * Setter method for instance variable {@link #extraLaborTime}.
         *
         * @param _extraLaborTime value for instance variable
         *            {@link #extraLaborTime}
         */
        public void setExtraLaborTime(final BigDecimal _extraLaborTime)
        {
            this.extraLaborTime = _extraLaborTime;
        }

        /**
         * Getter method for the instance variable {@link #nightLaborTime}.
         *
         * @return value of instance variable {@link #nightLaborTime}
         */
        public BigDecimal getNightLaborTime()
        {
            return this.nightLaborTime;
        }

        /**
         * Setter method for instance variable {@link #nightLaborTime}.
         *
         * @param _nightLaborTime value for instance variable
         *            {@link #nightLaborTime}
         */
        public void setNightLaborTime(final BigDecimal _nightLaborTime)
        {
            this.nightLaborTime = _nightLaborTime;
        }

        /**
         * Getter method for the instance variable {@link #holidayLaborTime}.
         *
         * @return value of instance variable {@link #holidayLaborTime}
         */
        public BigDecimal getHolidayLaborTime()
        {
            return this.holidayLaborTime;
        }

        /**
         * Setter method for instance variable {@link #holidayLaborTime}.
         *
         * @param _holidayLaborTime value for instance variable
         *            {@link #holidayLaborTime}
         */
        public void setHolidayLaborTime(final BigDecimal _holidayLaborTime)
        {
            this.holidayLaborTime = _holidayLaborTime;
        }
    }

    /**
     * Getter method for the instance variable {@link #emplInst}.
     *
     * @return value of instance variable {@link #emplInst}
     */
    public Instance getEmplInst()
    {
        return this.emplInst;
    }

    /**
     * Setter method for instance variable {@link #emplInst}.
     *
     * @param _emplInst value for instance variable {@link #emplInst}
     */
    public void setEmplInst(final Instance _emplInst)
    {
        this.emplInst = _emplInst;
    }

    /**
     * Getter method for the instance variable {@link #date}.
     *
     * @return value of instance variable {@link #date}
     */
    public DateTime getDate()
    {
        return this.date;
    }

    /**
     * Setter method for instance variable {@link #date}.
     *
     * @param _date value for instance variable {@link #date}
     */
    public void setDate(final DateTime _date)
    {
        this.date = _date;
    }


    /**
     * Getter method for the instance variable {@link #dueDate}.
     *
     * @return value of instance variable {@link #dueDate}
     */
    public DateTime getDueDate()
    {
        return this.dueDate;
    }


    /**
     * Setter method for instance variable {@link #dueDate}.
     *
     * @param _dueDate value for instance variable {@link #dueDate}
     */
    public void setDueDate(final DateTime _dueDate)
    {
        this.dueDate = _dueDate;
    }

}
