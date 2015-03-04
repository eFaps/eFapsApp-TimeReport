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

package org.efaps.esjp.timereport.reports;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ci.CITimeReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.AbstractGroupedByDate;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base.DateGroup;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.report.DocumentSumGroupedByDate_Base;
import org.efaps.esjp.sales.report.DocumentSumReport;
import org.efaps.esjp.timereport.listener.AbstractOnPayroll_Base.TimeBean;
import org.efaps.esjp.timereport.listener.OnPayslip;
import org.efaps.esjp.timereport.util.Timereport;
import org.efaps.esjp.timereport.util.TimereportSettings;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("7e371dff-34f3-4696-ae79-c539d6b22df2")
@EFapsRevision("$Rev: 4628 $")
public class PositionAnalyzeReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PositionAnalyzeReport.class);

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing html snipplet
     * @throws EFapsException on error
     */
    public Return generateReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the file
     * @throws EFapsException on error
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(DBProperties.getProperty(DocumentSumReport.class.getName() + ".FileName"));
        File file = null;
        if ("xls".equalsIgnoreCase(mime)) {
            file = dyRp.getExcel(_parameter);
        } else if ("pdf".equalsIgnoreCase(mime)) {
            file = dyRp.getPDF(_parameter);
        }
        ret.put(ReturnValues.VALUES, file);
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynPositionAnalyzeReport(this);
    }

    /**
     * Dynamic Report.
     */
    public static class DynPositionAnalyzeReport
        extends AbstractDynamicReport
    {

        /**
         * Filtered Report.
         */
        private final PositionAnalyzeReport_Base filterReport;

        /**
         * @param _positionAnalyseReport_Base
         */
        public DynPositionAnalyzeReport(final PositionAnalyzeReport_Base _positionAnalyseReport_Base)
        {
            this.filterReport = _positionAnalyseReport_Base;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<DataBean> beans = new ArrayList<>();
            final Map<String, Object> filter = getFilterReport().getFilterMap(_parameter);

            final boolean project = filter.containsKey("projectGroup")
                            && BooleanUtils.isTrue((Boolean) filter.get("projectGroup"));
            final boolean department = filter.containsKey("departmentGroup")
                            && BooleanUtils.isTrue((Boolean) filter.get("departmentGroup"));

            final AbstractGroupedByDate.DateGroup dateGroup = getDateGroup(_parameter);

            final PositionGroupedByDate group = new PositionGroupedByDate();
            final DateTimeFormatter dateTimeFormatter = group.getDateTimeFormatter(dateGroup);

            final QueryBuilder queryBldr = new QueryBuilder(CITimeReport.EmployeeTimeCardPosition);
            queryBldr.addType(CITimeReport.EmployeeAbsencePosition);
            add2QueryBuilder(_parameter, queryBldr);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selEmp = SelectBuilder.get()
                            .linkto(CITimeReport.EmployeeAbstractPosition.EmployeeAbstractLink);
            final SelectBuilder selEmpNum = new SelectBuilder(selEmp).attribute(CIHumanResource.Employee.Number);
            final SelectBuilder selAbsenceInst = SelectBuilder.get()
                            .linkto(CITimeReport.EmployeeAbstractPosition.AttrDefLinkAbstract).instance();
            final SelectBuilder selAbsenceValue = SelectBuilder.get()
                            .linkto(CITimeReport.EmployeeAbstractPosition.AttrDefLinkAbstract)
                            .attribute(CITimeReport.AttributeDefinitionAbstract.Value);
            multi.addSelect(selEmpNum, selAbsenceInst, selAbsenceValue);
            final SelectBuilder selDepName = new SelectBuilder(selEmp)
                            .linkfrom(CIHumanResource.Department2EmployeeAdminister.EmployeeLink)
                            .linkto(CIHumanResource.Department2EmployeeAdminister.DepartmentLink)
                            .attribute(CIHumanResource.Department.Name);
            if (department) {
                multi.addSelect(selDepName);
            }

            // HumanResource_EmployeeMsgPhrase
            final MsgPhrase emplPhrase = MsgPhrase.get(UUID.fromString("f543ca6d-29fb-4f1a-8747-0057b9a08404"));
            multi.addMsgPhrase(selEmp, emplPhrase);

            final SelectBuilder selProj = SelectBuilder.get()
                            .linkto(CITimeReport.EmployeeAbstractPosition.DocumentAbstractLink)
                            .linkfrom(CIProjects.Project2DocumentAbstract.ToAbstract)
                            .linkto(CIProjects.Project2DocumentAbstract.FromAbstract);
            MsgPhrase msgPhrase4Project = null;
            if (project) {
                // Project_ProjectMsgPhrase
                msgPhrase4Project = MsgPhrase.get(UUID.fromString("64c30826-cb22-4579-a3d5-bd10090f155e"));
                multi.addMsgPhrase(selProj, msgPhrase4Project);
            }

            multi.addAttribute(CITimeReport.EmployeeAbstractPosition.Date,
                            CITimeReport.EmployeeAbstractPosition.LaborTime,
                            CITimeReport.EmployeeAbstractPosition.ExtraLaborTime,
                            CITimeReport.EmployeeAbstractPosition.NightLaborTime,
                            CITimeReport.EmployeeAbstractPosition.HolidayLaborTime);
            multi.execute();
            while (multi.next()) {
                final DateTime date = multi.getAttribute(CITimeReport.EmployeeAbstractPosition.Date);
                final String partial = group.getPartial(date, dateGroup).toString(dateTimeFormatter);
                final String absence = multi.getSelect(selAbsenceValue);
                final DataBean bean = new DataBean()
                                .setPartial(partial)
                                .setEmployee(multi.getMsgPhrase(selEmp, emplPhrase))
                                .setEmployeeNumber(multi.<String>getSelect(selEmpNum))
                                .setAbsence(absence);
                if (multi.getCurrentInstance().getType().isCIType(CITimeReport.EmployeeAbsencePosition)) {
                    final Instance absenceInst = multi.getSelect(selAbsenceInst);
                    if (absenceInst != null && absenceInst.isValid()) {
                        final TimeBean timeBean = new TimeBean();
                        new OnPayslip().analyzeAbsence(_parameter, timeBean, absenceInst);
                        bean.setLaborTime(timeBean.getLaborTime());
                        bean.setExtraLaborTime(timeBean.getExtraLaborTime());
                        bean.setNightLaborTime(timeBean.getNightLaborTime());
                        bean.setHolidayLaborTime(timeBean.getHolidayLaborTime());
                    }
                } else {
                    bean.setLaborTime(getTime((Object[]) multi
                                                .getAttribute(CITimeReport.EmployeeTimeCardPosition.LaborTime)))
                                .setExtraLaborTime(getTime((Object[]) multi
                                                .getAttribute(CITimeReport.EmployeeTimeCardPosition.ExtraLaborTime)))
                                .setNightLaborTime(getTime((Object[]) multi
                                                .getAttribute(CITimeReport.EmployeeTimeCardPosition.NightLaborTime)))
                                .setHolidayLaborTime(getTime((Object[]) multi
                                                .getAttribute(CITimeReport.EmployeeTimeCardPosition.HolidayLaborTime)));
                }
                if (project) {
                    bean.setProject(multi.getMsgPhrase(selProj, msgPhrase4Project));
                }
                if (department) {
                    bean.setDepartment(multi.<String>getSelect(selDepName));
                }
                beans.add(bean);
            }
            return new JRBeanCollectionDataSource(beans);
        }

        protected DateGroup getDateGroup(final Parameter _parameter)
            throws EFapsException
        {
            DateGroup ret;
            final Map<String, Object> filter = getFilterReport().getFilterMap(_parameter);
            if (filter.containsKey("dateGroup") && filter.get("dateGroup") != null) {
                ret = (AbstractGroupedByDate.DateGroup) ((EnumFilterValue) filter.get("dateGroup")).getObject();
            } else {
                final Instance inst = _parameter.getInstance();
                if (inst != null && inst.getType().isKindOf(CIProjects.ProjectAbstract)) {
                    final String dgStr = Timereport.getSysConfig()
                                    .getAttributeValue(TimereportSettings.PARDATEGRP4PRJT);
                    if (dgStr == null) {
                        ret = DocumentSumGroupedByDate_Base.DateGroup.DAY;
                    } else {
                        ret = EnumUtils.getEnum(DocumentSumGroupedByDate_Base.DateGroup.class, dgStr);
                    }
                } else {
                    ret = DocumentSumGroupedByDate_Base.DateGroup.MONTH;
                }
            }
            return ret;
        }

        /**
         * @param _parameter
         * @param _queryBldr
         */
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilterReport().getFilterMap(_parameter);
            if (filterMap.containsKey("dateFrom")) {
                final DateTime date = (DateTime) filterMap.get("dateFrom");
                _queryBldr.addWhereAttrGreaterValue(CITimeReport.EmployeeAbstractPosition.Date,
                                date.withTimeAtStartOfDay().minusSeconds(1));
            }
            if (filterMap.containsKey("dateTo")) {
                final DateTime date = (DateTime) filterMap.get("dateTo");
                _queryBldr.addWhereAttrLessValue(CITimeReport.EmployeeAbstractPosition.Date,
                                date.withTimeAtStartOfDay().plusDays(1));
            }

            if (filterMap.containsKey("employee")) {
                final InstanceFilterValue filter = (InstanceFilterValue) filterMap.get("employee");
                if (filter.getObject() != null && filter.getObject().isValid()) {
                    _queryBldr.addWhereAttrEqValue(CITimeReport.EmployeeAbstractPosition.EmployeeAbstractLink,
                                    filter.getObject());
                }
            }

            final Instance inst = _parameter.getInstance();
            if (inst != null && inst.getType().isKindOf(CIProjects.ProjectAbstract)) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(
                                CITimeReport.Projects_ProjectService2EmployeeTimeCard);
                attrQueryBldr.addWhereAttrEqValue(CITimeReport.Projects_ProjectService2EmployeeTimeCard.FromLink, inst);
                _queryBldr.addWhereAttrInQuery(
                                CITimeReport.EmployeeAbstractPosition.DocumentAbstractLink,
                        attrQueryBldr.getAttributeQuery(CITimeReport.Projects_ProjectService2EmployeeTimeCard.ToLink));
            }
        }

        protected BigDecimal getTime(final Object[] _object)
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (_object != null) {
                final BigDecimal value = (BigDecimal) _object[0];
                if (value.compareTo(BigDecimal.ZERO) != 0) {
                    final UoM uoM = (UoM) _object[1];
                    if (uoM.equals(uoM.getDimension().getBaseUoM())) {
                        ret = value;
                    } else {
                        ret = BigDecimal.valueOf(uoM.getNumerator()).setScale(8, BigDecimal.ROUND_HALF_UP)
                                        .divide(BigDecimal.valueOf(uoM.getDenominator(), BigDecimal.ROUND_HALF_UP))
                                        .multiply(value);
                    }
                }
            }
            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilterReport().getFilterMap(_parameter);
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();
            if (filterMap.containsKey("projectGroup")) {
                if (BooleanUtils.isTrue((Boolean) filterMap.get("projectGroup"))) {
                    final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("project",
                                    String.class)
                                    .setHeaderWidth(150);
                    crosstab.addRowGroup(rowGroup);
                }
            }
            if (filterMap.containsKey("departmentGroup")) {
                if (BooleanUtils.isTrue((Boolean) filterMap.get("departmentGroup"))) {
                    final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("department",
                                    String.class);
                    crosstab.addRowGroup(rowGroup);
                }
            }

            final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("employee", String.class)
                            .setHeaderWidth(150);
            final CrosstabRowGroupBuilder<String> rowGroup2 = DynamicReports.ctab.rowGroup("employeeNumber",
                            String.class).setShowTotal(false);
            crosstab.addRowGroup(rowGroup, rowGroup2);

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class);
            crosstab.addColumnGroup(columnGroup);

            if (BooleanUtils.isTrue((Boolean) filterMap.get("absence"))
                            && getDateGroup(_parameter).equals(DocumentSumGroupedByDate_Base.DateGroup.DAY)) {
                final CrosstabMeasureBuilder<String> advanceMeasure = DynamicReports.ctab.measure(
                                getLabel("Absence"),
                                "absence", String.class, Calculation.NOTHING);
                crosstab.addMeasure(advanceMeasure);
            }

            final CrosstabMeasureBuilder<BigDecimal> laborTimeMeasure = DynamicReports.ctab.measure(
                            getLabel("LaborTime"),
                            "laborTime", BigDecimal.class, Calculation.SUM);
            final CrosstabMeasureBuilder<BigDecimal> extraLaborTimeMeasure = DynamicReports.ctab.measure(
                            getLabel("ExtraLaborTime"),
                            "extraLaborTime", BigDecimal.class, Calculation.SUM);
            final CrosstabMeasureBuilder<BigDecimal> nightLaborTimeMeasure = DynamicReports.ctab.measure(
                            getLabel("NightLaborTime"),
                            "nightLaborTime", BigDecimal.class, Calculation.SUM);
            final CrosstabMeasureBuilder<BigDecimal> holidayLaborTimeMeasure = DynamicReports.ctab.measure(
                            getLabel("HolidayLaborTime"),
                            "holidayLaborTime", BigDecimal.class, Calculation.SUM);
            crosstab.addMeasure(laborTimeMeasure, extraLaborTimeMeasure, nightLaborTimeMeasure, holidayLaborTimeMeasure);

            _builder.addSummary(crosstab);

        }

        protected String getLabel(final String _key)
        {
            return DBProperties.getProperty(PositionAnalyzeReport.class.getName() + "." + _key);
        }

        /**
         * Getter method for the instance variable {@link #filterReport}.
         *
         * @return value of instance variable {@link #filterReport}
         */
        public PositionAnalyzeReport_Base getFilterReport()
        {
            return this.filterReport;
        }
    }

    public static class DataBean
    {

        private String project;
        private String department;

        private String partial;
        private String employee;
        private String employeeNumber;

        private BigDecimal laborTime = BigDecimal.ZERO;
        private BigDecimal extraLaborTime = BigDecimal.ZERO;
        private BigDecimal nightLaborTime = BigDecimal.ZERO;
        private BigDecimal holidayLaborTime = BigDecimal.ZERO;

        private String absence;

        /**
         * Getter method for the instance variable {@link #partial}.
         *
         * @return value of instance variable {@link #partial}
         */
        public String getPartial()
        {
            return this.partial;
        }

        /**
         * Setter method for instance variable {@link #partial}.
         *
         * @param _partial value for instance variable {@link #partial}
         */
        public DataBean setPartial(final String _partial)
        {
            this.partial = _partial;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #employee}.
         *
         * @return value of instance variable {@link #employee}
         */
        public String getEmployee()
        {
            return this.employee;
        }

        /**
         * Setter method for instance variable {@link #employee}.
         *
         * @param _employee value for instance variable {@link #employee}
         */
        public DataBean setEmployee(final String _employee)
        {
            this.employee = _employee;
            return this;
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
        public DataBean setLaborTime(final BigDecimal _laborTime)
        {
            this.laborTime = _laborTime;
            return this;
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
        public DataBean setExtraLaborTime(final BigDecimal _extraLaborTime)
        {
            this.extraLaborTime = _extraLaborTime;
            return this;
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
        public DataBean setNightLaborTime(final BigDecimal _nightLaborTime)
        {
            this.nightLaborTime = _nightLaborTime;
            return this;
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
        public DataBean setHolidayLaborTime(final BigDecimal _holidayLaborTime)
        {
            this.holidayLaborTime = _holidayLaborTime;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #employeeNumber}.
         *
         * @return value of instance variable {@link #employeeNumber}
         */
        public String getEmployeeNumber()
        {
            return this.employeeNumber;
        }

        /**
         * Setter method for instance variable {@link #employeeNumber}.
         *
         * @param _employeeNumber value for instance variable
         *            {@link #employeeNumber}
         */
        public DataBean setEmployeeNumber(final String _employeeNumber)
        {
            this.employeeNumber = _employeeNumber;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #project}.
         *
         * @return value of instance variable {@link #project}
         */
        public String getProject()
        {
            return this.project;
        }

        /**
         * Setter method for instance variable {@link #project}.
         *
         * @param _project value for instance variable {@link #project}
         */
        public DataBean setProject(final String _project)
        {
            this.project = _project;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #department}.
         *
         * @return value of instance variable {@link #department}
         */
        public String getDepartment()
        {
            return this.department;
        }

        /**
         * Setter method for instance variable {@link #department}.
         *
         * @param _department value for instance variable {@link #department}
         */
        public DataBean setDepartment(final String _department)
        {
            this.department = _department;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #absence}.
         *
         * @return value of instance variable {@link #absence}
         */
        public String getAbsence()
        {
            return this.absence;
        }

        /**
         * Setter method for instance variable {@link #absence}.
         *
         * @param _advance value for instance variable {@link #absence}
         */
        public DataBean setAbsence(final String _advance)
        {
            this.absence = _advance;
            return this;
        }
    }

    public static class PositionGroupedByDate
        extends AbstractGroupedByDate
    {

    }
}
