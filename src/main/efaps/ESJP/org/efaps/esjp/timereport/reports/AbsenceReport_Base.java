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
package org.efaps.esjp.timereport.reports;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CITimeReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.AbstractGroupedByDate;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base.DateGroup;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.report.DocumentSumGroupedByDate_Base;
import org.efaps.esjp.sales.report.DocumentSumReport;
import org.efaps.esjp.timereport.reports.PositionAnalyzeReport_Base.PositionGroupedByDate;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("c1e293f0-43e0-4df5-97c3-a1b36d166376")
@EFapsApplication("eFapsApp-TimeReport")
public abstract class AbsenceReport_Base
    extends FilteredReport
{

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
        return new DynAbsenceReport(this);
    }

    /**
     * Dynamic Report.
     */
    public static class DynAbsenceReport
        extends AbstractDynamicReport
    {

        /**
         * Filtered Report.
         */
        private final AbsenceReport_Base filterReport;

        /**
         * @param _positionAnalyseReport_Base
         */
        public DynAbsenceReport(final AbsenceReport_Base _filterReport)
        {
            this.filterReport = _filterReport;
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
        public AbsenceReport_Base getFilterReport()
        {
            return this.filterReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<Map<String, ?>> source = new ArrayList<>();
            final AbstractGroupedByDate.DateGroup dateGroup = getDateGroup(_parameter);

            final PositionGroupedByDate group = new PositionGroupedByDate();
            final DateTimeFormatter dateTimeFormatter = group.getDateTimeFormatter(dateGroup);

            final QueryBuilder queryBldr = new QueryBuilder(CITimeReport.EmployeeAbsencePosition);
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

            // HumanResource_EmployeeMsgPhrase
            final MsgPhrase emplPhrase = MsgPhrase.get(UUID.fromString("f543ca6d-29fb-4f1a-8747-0057b9a08404"));
            multi.addMsgPhrase(selEmp, emplPhrase);

            multi.addAttribute(CITimeReport.EmployeeAbstractPosition.Date);
            multi.execute();
            while (multi.next()) {
                final Map<String, Object> map = new HashMap<>();
                source.add(map);

                final DateTime date = multi.getAttribute(CITimeReport.EmployeeAbstractPosition.Date);
                final String partial = group.getPartial(date, dateGroup).toString(dateTimeFormatter);
                final Instance absenceInst = multi.getSelect(selAbsenceInst);
                final String absence = multi.getSelect(selAbsenceValue);
                map.put("employee", multi.getMsgPhrase(selEmp, emplPhrase));
                map.put("partial", partial);
                map.put("employeeNumber", multi.<String>getSelect(selEmpNum));
                map.put(absenceInst.getOid(), absence);

            }
            return new JRMapCollectionDataSource(source);
        }

        protected DateGroup getDateGroup(final Parameter _parameter)
            throws EFapsException
        {
            DateGroup ret;
            final Map<String, Object> filter = getFilterReport().getFilterMap(_parameter);
            if (filter.containsKey("dateGroup") && filter.get("dateGroup") != null) {
                ret = (AbstractGroupedByDate.DateGroup) ((EnumFilterValue) filter.get("dateGroup")).getObject();
            } else {

                ret = DocumentSumGroupedByDate_Base.DateGroup.MONTH;

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

            final QueryBuilder attrQueryBldr = new QueryBuilder(CITimeReport.EmployeeTimeCard);
            attrQueryBldr.addWhereAttrNotEqValue(CITimeReport.EmployeeTimeCard.Status,
                            Status.find(CITimeReport.EmployeeTimeCardStatus.Canceled));
            _queryBldr.addWhereAttrInQuery(CITimeReport.EmployeeAbstractPosition.DocumentAbstractLink,
                            attrQueryBldr.getAttributeQuery(CITimeReport.EmployeeTimeCard.ID));
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();
            final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("employee", String.class)
                            .setHeaderWidth(150);
            final CrosstabRowGroupBuilder<String> rowGroup2 = DynamicReports.ctab.rowGroup("employeeNumber",
                            String.class).setShowTotal(false);
            crosstab.addRowGroup(rowGroup, rowGroup2);

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class);
            crosstab.addColumnGroup(columnGroup);

            final QueryBuilder queryBldr = new QueryBuilder(CITimeReport.AttributeDefinitionAbsenceReason);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CITimeReport.AttributeDefinitionAbsenceReason.Value);
            multi.execute();
            while (multi.next()) {
                final CrosstabMeasureBuilder<String> absenceMeasure = DynamicReports.ctab.measure(
                                multi.<String>getAttribute(CITimeReport.AttributeDefinitionAbsenceReason.Value),
                                multi.getCurrentInstance().getOid(), String.class, Calculation.COUNT);
                crosstab.addMeasure(absenceMeasure);
            }
            _builder.addSummary(crosstab);
        }
    }
}
