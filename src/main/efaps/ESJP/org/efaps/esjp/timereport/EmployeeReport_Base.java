/*
 * Copyright 2003 - 2010 The eFaps Team
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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.StringEscapeUtils;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ci.CITimeReport;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("5626aabf-4f61-4844-8bca-498e65a878c6")
@EFapsRevision("$Rev$")
public abstract class EmployeeReport_Base
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
        final Instance repInst = createBaseReport(_parameter);
        createPositions(_parameter, repInst);
        return new Return();
    }


    /**
     * Create the base report.
     * @param _parameter Parameter as passed form the eFasp API
     * @return Instance of the new Report
     * @throws EFapsException on error
     */
    protected Instance createBaseReport(final Parameter _parameter)
        throws EFapsException
    {
        final String name = _parameter.getParameterValue("name");
        final String date = _parameter.getParameterValue("date");
        final String duedate = _parameter.getParameterValue("dueDate");
        final String salesperson = _parameter.getParameterValue("salesperson");
        final Insert insert = new Insert(CITimeReport.EmployeeReport);
        insert.add(CITimeReport.EmployeeReport.Name, name);
        insert.add(CITimeReport.EmployeeReport.Date, date);
        insert.add(CITimeReport.EmployeeReport.DueDate, duedate);
        insert.add(CITimeReport.EmployeeReport.Salesperson, salesperson);
        insert.add(CITimeReport.EmployeeReport.Status,
                                            Status.find(CITimeReport.EmployeeReportStatus.uuid, "Open").getId());
        insert.execute();
        return insert.getInstance();
    }

    /**
     * Create the positions for the report.
     * @param _parameter    Parameter as passed form the eFasp API
     * @param _reportInst   Instance of the report this positions belong to
     * @return list of newly created instances
     * @throws EFapsException on error
     */
    protected List<Instance> createPositions(final Parameter _parameter,
                                             final Instance _reportInst)
        throws EFapsException
    {
        final List<Instance> ret = new ArrayList<Instance>();
        final String[] employees = _parameter.getParameterValues("employeeLink");
        final String[] quantity = _parameter.getParameterValues("quantity");
        final String[] quantityUoM = _parameter.getParameterValues("quantityUoM");
        final String[] categories = _parameter.getParameterValues("categoryAbstractLink");
        final String[] categoryDesc = _parameter.getParameterValues("categoryDesc");
        final String[] dates = _parameter.getParameterValues("date");

        for (int i = 0; i < employees.length; i++) {
            if (employees[i] != null && !employees[i].isEmpty()
                            && quantity[i] != null && !quantity[i].isEmpty()
                            && categories[i] != null && !categories[i].isEmpty()) {
                final Insert posInsert = new Insert(CITimeReport.EmployeeReportPosition);
                posInsert.add(CITimeReport.EmployeeReportPosition.ReportLink, _reportInst.getId());
                posInsert.add(CITimeReport.EmployeeReportPosition.EmployeeLink, Instance.get(employees[i]).getId());
                posInsert.add(CITimeReport.EmployeeReportPosition.PositionNumber, i + 1);
                posInsert.add(CITimeReport.EmployeeReportPosition.Quantity, new Object[] { quantity[i], quantityUoM[i] });
                posInsert.add(CITimeReport.EmployeeReportPosition.CategoryAbstractLink, categories[i]);
                if (categoryDesc != null) {
                    posInsert.add(CITimeReport.EmployeeReportPosition.CategoryDesc, categoryDesc[i]);
                }
                if (dates != null) {
                    posInsert.add(CITimeReport.EmployeeReportPosition.Date, dates[i]);
                }
                posInsert.execute();
                ret.add(posInsert.getInstance());
            }
        }
        return ret;
    }

    /**
     * Validate the parametets given form the create form.
     * @param _parameter    Parameter as passed form the eFasp API
     * @return Return
     * @throws EFapsException on error
     */
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret= new Return();
        final StringBuilder html = validatePositions(_parameter);
        if (html.length() > 0) {
            ret.put(ReturnValues.SNIPLETT, html.toString());
        } else {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }


    protected StringBuilder validatePositions(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final String[] employees = _parameter.getParameterValues("employeeLink");
        final String[] quantity = _parameter.getParameterValues("quantity");
        final String[] categories = _parameter.getParameterValues("categoryAbstractLink");
        final String[] projects = _parameter.getParameterValues("projects");

        for (int i = 0; i < employees.length; i++) {
            final boolean[] valid = new boolean[4];
            if (employees[i] == null || employees[i].isEmpty()) {
                valid[0] = false;
            } else {
                valid[0] = true;
            }
            if (quantity[i] == null || quantity[i].isEmpty()) {
                valid[1] = false;
            } else {
                valid[1] = true;
            }
            if (projects[i] == null || projects[i].isEmpty()) {
                valid[2] = false;
            } else {
                valid[2] = true;
            }
            if (categories[i] == null || categories[i].isEmpty()) {
                valid[3] = false;
            } else {
                valid[3] = true;
            }
            if (valid[0] == false || valid[1] == false || valid[2] == false || valid[3] == false) {
                if (ret.length() == 0) {
                    ret.append("<table>")
                        .append("<tr><th></th><th>")
                        .append(DBProperties.getProperty("TimeReport_EmployeeReportPosition.employeeLink.Label"))
                        .append("</th><th>")
                        .append(DBProperties.getProperty("TimeReport_EmployeeReportPosition/Quantity.Label"))
                        .append("</th><th>")
                        .append(DBProperties.getProperty("TimeReport_EmployeeReportPositionTable.projects.Label"))
                        .append("</th><th>")
                        .append(DBProperties.getProperty(
                                        "TimeReport_EmployeeReportPosition.categoryAbstractLink.Label"))
                        .append("</th></tr>");
                }
                ret.append("<tr><td>").append(i + 1).append("</td>");
                for (int j = 0 ; j < 4 ; j++) {
                    ret.append("<td>").append(valid[j] ? "" : "X").append("</td>");
                }
                ret.append("</tr>");
            }
        }
        if (ret.length() > 0) {
            ret.append("</table>");
        }
        return ret;
    }

    /**
     * Method for create a new Category to Project.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return createCategory4Project(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        final String name = _parameter.getParameterValue("name");
        final String description = _parameter.getParameterValue("description");
        final String status = _parameter.getParameterValue("active");

        final Insert insert = new Insert(CITimeReport.CategoryEmployee);
        insert.add(CITimeReport.CategoryEmployee.Name, name);
        insert.add(CITimeReport.CategoryEmployee.Description, description);
        insert.add(CITimeReport.CategoryEmployee.ProjectLink, instance.getId());
        insert.add(CITimeReport.CategoryEmployee.Active, status);
        insert.execute();

        return new Return();
    }

    /**
     * Create a EmployeeReportPosition.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return
     * @throws EFapsException on error
     */
    public Return createEmployee(final Parameter _parameter)
        throws EFapsException
    {
        final Instance inst = _parameter.getInstance();
        final String employees = _parameter.getParameterValue("employeeLink");
        final String quantity = _parameter.getParameterValue("quantity");
        final String quantityUoM = _parameter.getParameterValue("quantityUoM");
        final String categoryDesc = _parameter.getParameterValue("categoryDesc");
        final String date = _parameter.getParameterValue("date");

        final Insert posInsert = new Insert(CITimeReport.EmployeeReportPosition);
        posInsert.add(CITimeReport.EmployeeReportPosition.ReportLink, 0);
        posInsert.add(CITimeReport.EmployeeReportPosition.EmployeeLink, Instance.get(employees).getId());
        posInsert.add(CITimeReport.EmployeeReportPosition.PositionNumber, 0);
        posInsert.add(CITimeReport.EmployeeReportPosition.Quantity, new Object[] { quantity, quantityUoM });
        posInsert.add(CITimeReport.EmployeeReportPosition.CategoryAbstractLink, inst.getId());
        if (categoryDesc != null) {
            posInsert.add(CITimeReport.EmployeeReportPosition.CategoryDesc, categoryDesc);
        }
        if (date != null) {
            posInsert.add(CITimeReport.EmployeeReportPosition.Date, date);
        }
        posInsert.execute();

        return new Return();
    }

    /**
     * Create a EmployeeReportPosition.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return
     * @throws EFapsException on error
     */
    public Return createPosition(final Parameter _parameter)
        throws EFapsException
    {
        final Instance inst = _parameter.getInstance();
        final String employees = _parameter.getParameterValue("employeeLink");
        final String quantity = _parameter.getParameterValue("quantity");
        final String quantityUoM = _parameter.getParameterValue("quantityUoM");
        final String categories = _parameter.getParameterValue("categoryAbstractLink");
        final String categoryDesc = _parameter.getParameterValue("categoryDesc");
        final String date = _parameter.getParameterValue("date");

        if (inst != null && inst.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CITimeReport.EmployeeReportPosition);
            queryBldr.addWhereAttrEqValue(CITimeReport.EmployeeReportPosition.ReportLink, inst.getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CITimeReport.EmployeeReportPosition.PositionNumber);
            multi.execute();
            int newPos = 0;
            while (multi.next()) {
                newPos = multi.<Integer>getAttribute(CITimeReport.EmployeeReportPosition.PositionNumber);
            }

            final Insert posInsert = new Insert(CITimeReport.EmployeeReportPosition);
            posInsert.add(CITimeReport.EmployeeReportPosition.ReportLink, inst.getId());
            posInsert.add(CITimeReport.EmployeeReportPosition.EmployeeLink, Instance.get(employees).getId());
            posInsert.add(CITimeReport.EmployeeReportPosition.PositionNumber, newPos + 1);
            posInsert.add(CITimeReport.EmployeeReportPosition.Quantity, new Object[] { quantity, quantityUoM });
            posInsert.add(CITimeReport.EmployeeReportPosition.CategoryAbstractLink, categories);
            if (categoryDesc != null) {
                posInsert.add(CITimeReport.EmployeeReportPosition.CategoryDesc, categoryDesc);
            }
            if (date != null) {
                posInsert.add(CITimeReport.EmployeeReportPosition.Date, date);
            }
            posInsert.execute();
        }

        return new Return();
    }

    /**
     * Method for auto-complete projects.
     * @param _parameter Parameter as passed from the eFaps API.
     * @return ret Return values of the list.
     * @throws EFapsException on error
     */
    public Return autoComplete4Projects(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String key = (String) properties.get("key");
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        if (input != null & !input.isEmpty()) {
            final Map<String, Map<String, String>> tmpMap = new TreeMap<String, Map<String, String>>();
            final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProjectService);
            queryBldr.setOr(true);
            queryBldr.addWhereAttrMatchValue(CIProjects.ProjectService.Name, input + "*").setIgnoreCase(true);
            queryBldr.addWhereAttrMatchValue(CIProjects.ProjectService.Description, input + "*").setIgnoreCase(true);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProjects.ProjectService.Name,
                               CIProjects.ProjectService.Description);
            multi.execute();
            while (multi.next()) {
                final String name = multi.<String>getAttribute(CIProjects.ProjectService.Name);
                final String desc = multi.<String>getAttribute(CIProjects.ProjectService.Description);
                final String choice = name + " - " + desc;
                final Map<String, String> map = new HashMap<String, String>();
                map.put("eFapsAutoCompleteKEY", "ID".equalsIgnoreCase(key)
                                                ? ((Long) multi.getCurrentInstance().getId()).toString()
                                                : multi.getCurrentInstance().getOid());
                map.put("eFapsAutoCompleteVALUE", name);
                map.put("eFapsAutoCompleteCHOICE", choice);
                tmpMap.put(choice, map);
            }
            list.addAll(tmpMap.values());
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Method to update projects and show any drop-down.
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list with values.
     * @throws EFapsException on error.
     */
    public Return updateFields4Projects(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final Map<String, Long> map = new TreeMap<String, Long>();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Integer pos = (_parameter.getParameterValue("eFapsRowSelectedRow") != null
                        ? Integer.parseInt(_parameter.getParameterValue("eFapsRowSelectedRow"))
                                        : 0);
        final String project = _parameter.getParameterValues("projects")[pos];
        if (project != null && !project.isEmpty()) {
            final QueryBuilder queryBldr = new QueryBuilder(CITimeReport.CategoryEmployee);
            queryBldr.addWhereAttrEqValue(CITimeReport.CategoryEmployee.ProjectLink, Instance.get(project).getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CITimeReport.CategoryEmployee.ID, CITimeReport.CategoryEmployee.Name);
            multi.execute();
            while (multi.next()) {
                final long id = multi.<Long>getAttribute(CITimeReport.CategoryEmployee.ID);
                final String name = multi.<String>getAttribute(CITimeReport.CategoryEmployee.Name);
                map.put(name, id);
            }
            final Map<String, String> mapValue = new HashMap<String, String>();
            final StringBuilder js = new StringBuilder();
            if (!map.isEmpty()) {
                js.append("new Array('").append(1).append("'");
                for (final Entry<String, Long> entry : map.entrySet()) {
                    js.append(",'").append(entry.getValue()).append("','")
                                   .append(StringEscapeUtils.escapeJavaScript(entry.getKey())).append("'");
                }
                js.append(")");
            } else {
                js.append("new Array('").append(0).append("')");
            }
            mapValue.put("categoryAbstractLink", js.toString());
            list.add(mapValue);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list with values.
     * @throws EFapsException on error.
     */
    public Return getResumeUIFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder();

        final QueryBuilder queryBldr = new QueryBuilder(CITimeReport.EmployeeReportAbstractPosition);
        queryBldr.addWhereAttrEqValue(CITimeReport.EmployeeReportAbstractPosition.DocumentAbstractLink,
                        _parameter.getInstance().getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selEmp = new SelectBuilder().linkto(CITimeReport.EmployeeReportPosition.EmployeeAbstractLink);
        final SelectBuilder selEmpLastName = new SelectBuilder(selEmp).attribute(CIHumanResource.Employee.LastName);
        final SelectBuilder selEmpFirstName = new SelectBuilder(selEmp).attribute(CIHumanResource.Employee.FirstName);
        final SelectBuilder selEmpOid = new SelectBuilder(selEmp).oid();
        multi.addSelect(selEmpOid, selEmpLastName, selEmpFirstName);
        multi.addAttribute(CITimeReport.EmployeeReportPosition.Date, CITimeReport.EmployeeReportPosition.Quantity);
        multi.execute();
        final Map<Instance, EmpReportPos> values = new HashMap<Instance, EmpReportPos>();
        final List<EmpReportPos> employees = new ArrayList<EmpReportPos>();
        while (multi.next()) {
            final Instance emplInst = Instance.get((String) multi.getSelect(selEmpOid));
            final Object[] quant = multi.getAttribute(CITimeReport.EmployeeReportPosition.Quantity);
            final DateTime date = multi.<DateTime>getAttribute(CITimeReport.EmployeeReportPosition.Date);
            EmpReportPos pos;
            if (values.containsKey(emplInst)) {
                pos = values.get(emplInst);
            } else {
                final String lastName = multi.<String>getSelect(selEmpLastName);
                final String firstName = multi.<String>getSelect(selEmpFirstName);
                pos = new EmpReportPos(emplInst, lastName + ", " + firstName);
                employees.add(pos);
            }
            values.put(emplInst, pos);
            pos.add(date, (BigDecimal) quant[0], (UoM) quant[1]);
        }
        final Set<DateTime> dates = new TreeSet<DateTime>();
        for (final EmpReportPos pos : values.values()) {
            for (final DateTime date : pos.getDate2quantity().keySet()) {
                dates.add(date);
            }
        }
        Collections.sort(employees, new Comparator<EmpReportPos>() {

            @Override
            public int compare(final EmpReportPos _o1,
                               final EmpReportPos _o2)
            {
                return _o1.getName().compareTo(_o2.getName());
            }});
        html.append("<table><tr><th>")
            .append(DBProperties.getProperty("org.efaps.esjp.timereport.EmployeeReport.getResumeUIFieldValue.employee"))
            .append("</th>");
        final DateTimeFormatter fomatr = DateTimeFormat.forStyle("M-").withLocale(
                        Context.getThreadContext().getLocale());

        for (final DateTime date : dates) {
            html.append("<th>").append(fomatr.print(date)).append("</th>");
        }
        html.append("</tr>");
        final DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        format.applyPattern("#,##0.##");
        for (final EmpReportPos pos : employees) {
            html.append("<tr>").append("<td>").append(pos.getName()).append("</td>");
            for (final DateTime date : dates) {
                html.append("<td>");
                if (pos.getDate2quantity().containsKey(date)) {
                    html.append(format.format(pos.getDate2quantity().get(date))).append(pos.getUom().getName());
                }
                html.append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</table>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    public class EmpReportPos
    {

        private final String name;

        private UoM uom = null;

        private final Map<DateTime, BigDecimal> date2quantity = new HashMap<DateTime, BigDecimal>();


        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        protected String getName()
        {
            return this.name;
        }


        /**
         * Getter method for the instance variable {@link #uom}.
         *
         * @return value of instance variable {@link #uom}
         */
        protected UoM getUom()
        {
            return this.uom;
        }


        /**
         * Getter method for the instance variable {@link #date2quantity}.
         *
         * @return value of instance variable {@link #date2quantity}
         */
        protected Map<DateTime, BigDecimal> getDate2quantity()
        {
            return this.date2quantity;
        }

        /**
         * @param _emplInst
         * @param _string
         */
        public EmpReportPos(final Instance _emplInst,
                            final String _name)
        {
            this.name = _name;
        }

        /**
         * @param _date
         * @param _quant2
         * @param _quant
         */
        public void add(final DateTime _date,
                        final BigDecimal _quantity,
                        final UoM _uoM)
        {
            if (this.uom == null) {
                this.uom = _uoM.getDimension().getBaseUoM();
            }
            BigDecimal quantity = BigDecimal.ZERO;
            if (this.date2quantity.containsKey(_date)) {
                quantity = this.date2quantity.get(_date);
            }
            quantity = quantity.add(new BigDecimal(_uoM.getNumerator()).setScale(8).divide(
                            new BigDecimal(_uoM.getDenominator()), BigDecimal.ROUND_HALF_UP).multiply(_quantity));
            this.date2quantity.put(_date, quantity);
        }

    }
}
