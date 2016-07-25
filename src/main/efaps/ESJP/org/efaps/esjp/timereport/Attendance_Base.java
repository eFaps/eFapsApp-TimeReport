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
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIFormTimeReport;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CITimeReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.common.uiform.Field_Base.ListType;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractValueFormatter;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.datatype.DateType;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Attendance_Base.java 10136 2013-08-28 02:53:51Z jan@moxter.net
 *          $
 */
@EFapsUUID("5e8e3fe4-a95d-4381-aa47-2684c9d4368e")
@EFapsApplication("eFapsApp-TimeReport")
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
        final List<DropDownPosition> values = new ArrayList<>();
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

    public Return attendanceReportUIFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AttendanceReport report = new AttendanceReport();
        ret.put(ReturnValues.SNIPLETT, report.getHtmlSnipplet(_parameter));
        return ret;
    }

    public static class AttendanceReport
        extends AbstractDynamicReport
    {

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final DateTime dateFrom = new DateTime(_parameter.getParameterValue(
                            CIFormTimeReport.TimeReport_AttendanceSelfRegistrationReportSelectForm.dateFrom.name));
            final DateTime dateUntil = new DateTime(_parameter.getParameterValue(
                            CIFormTimeReport.TimeReport_AttendanceSelfRegistrationReportSelectForm.dateUntil.name));
            final Map<LocalDate, AttendanceTime> values = new TreeMap<>();

            final QueryBuilder queryBuilder = new QueryBuilder(CITimeReport.AttendanceAbstract);
            queryBuilder.addWhereAttrGreaterValue(CITimeReport.AttendanceAbstract.Time, dateFrom.minusMinutes(1));
            queryBuilder.addWhereAttrLessValue(CITimeReport.AttendanceAbstract.Time, dateUntil.plusDays(1));

            final Instance employeeInst = Instance.get(_parameter.getParameterValue(
                            CIFormTimeReport.TimeReport_AttendanceSelfRegistrationReportSelectForm.employee.name));
            if (employeeInst.isValid()) {
                queryBuilder.addWhereAttrEqValue(CITimeReport.AttendanceAbstract.EmployeeAbstractLink, employeeInst);
            } else {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIHumanResource.Employee);
                attrQueryBldr.addWhereAttrEqValue(CIHumanResource.Employee.UserPerson,
                                Context.getThreadContext().getPersonId());
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIHumanResource.Employee.ID);
                queryBuilder.addWhereAttrInQuery(CITimeReport.AttendanceAbstract.EmployeeAbstractLink, attrQuery);
            }

            final MultiPrintQuery multi = queryBuilder.getPrint();
            multi.addAttribute(CITimeReport.AttendanceAbstract.Time);
            multi.execute();
            while (multi.next()) {
                final Instance instance = multi.getCurrentInstance();
                final DateTime dateTime = multi.<DateTime>getAttribute(CITimeReport.AttendanceAbstract.Time);
                final LocalDate localdate = dateTime.toLocalDate();
                AttendanceTime aTime;
                if (values.containsKey(localdate)) {
                    aTime = values.get(localdate);
                } else {
                    aTime = new AttendanceTime(localdate);
                    values.put(localdate, aTime);
                }
                if (instance.getType().isKindOf(CITimeReport.AttendanceStartAbstract.getType())) {
                    aTime.addStart(dateTime);
                } else {
                    aTime.addEnd(dateTime);
                }
            }

            final DRDataSource dataSource = new DRDataSource("month", "week", "day", "hourQuantity", "minuteQuantity",
                            "minutes");
            for (final Entry<LocalDate, AttendanceTime> entry : values.entrySet()) {
                final Date date = entry.getKey().toDate();
                dataSource.add(date, entry.getValue().getWeek(), date, entry.getValue().getHours(), entry.getValue()
                                .getMinutePart(), entry.getValue().getMinutes());
            }
            return dataSource;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<Date> monthColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.timereport.Attendance.AttendanceReport.month"), "month",
                            DynamicReports.type.dateMonthType());

            final TextColumnBuilder<String> weekColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.timereport.Attendance.AttendanceReport.week"), "week",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<Date> dayColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.timereport.Attendance.AttendanceReport.day"), "day",
                            new DateType()
                            {

                                private static final long serialVersionUID = 1L;

                                @Override
                                public String getPattern()
                                {
                                    return "E";
                                }
                            });
            final TextColumnBuilder<Long> hourQuantityColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.timereport.Attendance.AttendanceReport.hourQuantity"),
                            "hourQuantity",
                            DynamicReports.type.longType());
            final TextColumnBuilder<Long> minuteQuantityColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.timereport.Attendance.AttendanceReport.minuteQuantity"),
                            "minuteQuantity",
                            DynamicReports.type.longType());

            final ColumnGroupBuilder monthGroup = DynamicReports.grp.group(monthColumn).groupByDataType();

            final ColumnGroupBuilder weekGroup = DynamicReports.grp.group(weekColumn).groupByDataType();

            final AggregationSubtotalBuilder<Long> hourSum = DynamicReports.sbt.sum("minutes", Long.class,
                            hourQuantityColumn);
            hourSum.setValueFormatter(new HourFormater());

            final AggregationSubtotalBuilder<Long> monthHourSum = DynamicReports.sbt.sum("minutes", Long.class,
                            hourQuantityColumn);
            monthHourSum.setValueFormatter(new HourFormater());

            final AggregationSubtotalBuilder<Long> minuteSum = DynamicReports.sbt.sum("minutes", Long.class,
                            minuteQuantityColumn);
            minuteSum.setValueFormatter(new MinuteFormater());

            final AggregationSubtotalBuilder<Long> monthMinuteSum = DynamicReports.sbt.sum("minutes", Long.class,
                            minuteQuantityColumn);
            monthMinuteSum.setValueFormatter(new MinuteFormater());

            _builder.addColumn(monthColumn, weekColumn, dayColumn, hourQuantityColumn, minuteQuantityColumn)
                            .addGroup(monthGroup, weekGroup)
                            .addSubtotalAtGroupFooter(weekGroup, hourSum, minuteSum)
                            .addSubtotalAtGroupFooter(monthGroup, monthHourSum, monthMinuteSum);
        }

    }

    public static class HourFormater
        extends AbstractValueFormatter<Long, Long>
    {

        private static final long serialVersionUID = 1L;

        @Override
        public Long format(final Long _value,
                           final ReportParameters _reportParameters)
        {
            return _value / 60;
        }
    }

    public static class MinuteFormater
        extends AbstractValueFormatter<Long, Long>
    {

        private static final long serialVersionUID = 1L;

        @Override
        public Long format(final Long _value,
                           final ReportParameters _reportParameters)
        {
            return _value % 60;
        }
    }

    public static class AttendanceTime
    {

        private final List<DateTime> starts = new ArrayList<>();
        private final List<DateTime> ends = new ArrayList<>();

        private Long hours;
        private Long minutes;
        private final LocalDate localDate;

        /**
         * @param _localdate
         */
        public AttendanceTime(final LocalDate _localdate)
        {
            this.localDate = _localdate;
        }

        /**
         * @param _dateTime
         */
        public void addStart(final DateTime _dateTime)
        {
            this.starts.add(_dateTime);
        }

        /**
         * @param _dateTime
         */
        public void addEnd(final DateTime _dateTime)
        {
            this.ends.add(_dateTime);
        }

        public Long getHours()
        {
            if (this.hours == null) {
                calculate();
            }
            return this.hours;
        }

        public Long getMinutes()
        {
            if (this.minutes == null) {
                calculate();
            }
            return this.minutes;
        }

        public Long getMinutePart()
        {
            return this.minutes % 60;
        }

        private void calculate()
        {
            this.hours = Long.valueOf(0);
            this.minutes = Long.valueOf(0);
            Collections.sort(this.starts);
            Collections.sort(this.ends);
            final Iterator<DateTime> startIter = this.starts.iterator();
            final Iterator<DateTime> endIter = this.ends.iterator();
            DateTime currentEnd = null;
            while (startIter.hasNext()) {
                // set the start and end
                final DateTime start = startIter.next();
                if (endIter.hasNext()) {
                    currentEnd = endIter.next();
                }
                if (currentEnd != null) {
                    if (start.isBefore(currentEnd)) {
                        final Duration duration = new Duration(start, currentEnd);
                        this.hours = this.hours + duration.getStandardHours();
                        this.minutes = this.minutes + duration.getStandardMinutes();
                    }
                }
            }
        }

        public String getWeek()
        {
            final StringBuilder bldr = new StringBuilder();
            final LocalDate mon = this.localDate.withDayOfWeek(1);
            final LocalDate sun = this.localDate.withDayOfWeek(7);
            try {
                final DateTimeFormatter formatter = DateTimeFormat.shortDate().withLocale(
                                Context.getThreadContext().getLocale());
                bldr.append(mon.toString(formatter)).append(" - ")
                                .append(sun.toString(formatter));
            } catch (final EFapsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return bldr.toString();
        }
    }
}
