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

package org.efaps.esjp.timereport;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormTimeReport;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ci.CITimeReport;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.common.util.InterfaceUtils_Base.DojoLibs;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("bc1ebe71-6af7-4ef8-b9b8-7ebcb61432db")
@EFapsRevision("$Rev: 4628 $")
public abstract class EmployeeTimeCard_Base
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

    public Return create4Project(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProjectService2Employee);
        queryBldr.addWhereAttrEqValue(CIProjects.ProjectService2Employee.Status,
                        Status.find(CIProjects.ProjectService2EmployeeStatus.Active));

        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selEmplInst = SelectBuilder.get().linkto(CIProjects.ProjectService2Employee.ToLink)
                        .instance();
        final SelectBuilder selProjInst = SelectBuilder.get().linkto(CIProjects.ProjectService2Employee.FromLink)
                        .instance();
        multi.addSelect(selEmplInst, selProjInst);
        multi.execute();
        while (multi.next()) {
            final Instance employinst = multi.getSelect(selEmplInst);
            final Instance projInst = multi.getSelect(selProjInst);
            if (employinst.isValid() && projInst.isValid()) {
                final Parameter parameter = ParameterUtil.clone(_parameter);
                ParameterUtil.setParmeterValue(parameter,
                                CIFormTimeReport.TimeReport_EmployeeTimeCardForm.employee.name,
                                employinst.getOid());
                ParameterUtil.setParmeterValue(parameter,
                                CIFormTimeReport.TimeReport_EmployeeTimeCardForm.project.name,
                                projInst.getOid());
                create(parameter);
            }
        }

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
                add2createDoc(_parameter, _insert);
            }
        };

        final Instance docInst = create.basicInsert(_parameter);
        ret.setInstance(docInst);
        create.connect(_parameter, docInst);

        return ret;
    }

    protected void add2createDoc(final Parameter _parameter,
                                 final Insert _insert)
        throws EFapsException
    {
        _insert.add(CITimeReport.EmployeeTimeCard.Name, getDocName4Create(_parameter));
        _insert.add(CITimeReport.EmployeeTimeCard.EmployeeLink, Instance.get(_parameter
                        .getParameterValue(CIFormTimeReport.TimeReport_EmployeeTimeCardForm.employee.name)));
    }

    /**
     * Create a EmployeeReport.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
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
                print.addAttribute(CITimeReport.EmployeeTimeCard.EmployeeLink);
                print.execute();
                _insert.add(CITimeReport.EmployeeTimeCardPosition.EmployeeLink,
                                print.getAttribute(CITimeReport.EmployeeTimeCard.EmployeeLink));
            }
        };
        return create.execute(_parameter);
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing Snipplet
     * @throws EFapsException on error
     */
    public Return getRegisterUIFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final DateTime fromDate = new DateTime(_parameter.getParameterValue(
                        CIFormTimeReport.TimeReport_EmployeeTimeCardRegisterSelectForm.fromDate.name));
        final DateTime toDate = new DateTime(_parameter.getParameterValue(
                        CIFormTimeReport.TimeReport_EmployeeTimeCardRegisterSelectForm.toDate.name));
        final DateTimeFormatter dateFormatter = DateTimeFormat.shortDate().withLocale(
                        Context.getThreadContext().getLocale());

        final List<LocalDate> dates = new ArrayList<>();
        DateTime date = fromDate;
        while (date.isBefore(toDate)) {
            dates.add(date.toLocalDate());
            date = date.plusDays(1);
        }
        if (!toDate.toLocalDate().equals(fromDate.toLocalDate())) {
            dates.add(toDate.toLocalDate());
        }

        final StringBuilder script = new StringBuilder()
            .append("var fsStore = new ItemFileReadStore({ data : {identifier:'value', label: 'name', items: [{value: 1, name: 'A'}, {value: 2,name: 'B'}, {value: 3, name: 'C'}]}}); ")
            .append("")
            .append("var layout = [{")
            .append("onBeforeRow : function(inDataIndex, inSubRows)")
            .append("{")
            .append("  inSubRows[0].invisible = inDataIndex >= 0;")
            .append("}, ")
            .append("noscroll: true,")
            .append("cells: [")
            .append("[")
            .append("{ name: 'Documento', colSpan: 3, width: 'auto'}")
            .append("],[")
            .append("{ name: 'No', field: 'name', width: '50px'},")
            .append("{ name: 'DNI',field: 'number', width: '50px'},")
            .append("{ name: 'Empleado',field: 'employee', width: '250px'}")
            .append("]")
            .append("]},{")
            .append("onBeforeRow : function(inDataIndex, inSubRows)")
            .append("{")
            .append("  inSubRows[0].invisible = inDataIndex >= 0;")
            .append("}, ")
            .append("defaultCell: { width: \"25px\" },")
            .append("cells: [")
            .append("[");
        boolean first = true;
        for (final LocalDate localDate : dates) {
            if (first) {
                first = false;
            } else {
                script.append(",");
            }
            script.append("{headerClasses : 'staticHeader', classes: 'staticHeader', colSpan: 5, name: '")
                            .append(localDate.toString(dateFormatter)).append("'}");
        }
        script.append("],[");
        first = true;
        for (final LocalDate localDate : dates) {
            if (first) {
                first = false;
            } else {
                script.append(",");
            }
            script
                .append("{ editable: true, name: '")
                .append(DBProperties.getProperty(EmployeeTimeCard.class.getName() + ".AbsenceReason"))
                .append("', field: 'AR_").append(localDate.getDayOfYear()).append("', type: dojox.grid.cells._Widget, ")
                .append("widgetClass: dijit.form.FilteringSelect, widgetProps: {store: fsStore}")
                .append("},")
                .append("{ editable: true,  name: '")
                .append(DBProperties.getProperty(EmployeeTimeCard.class.getName() + ".LaborTime"))
                .append("', field: 'LT_").append(localDate.getDayOfYear()).append("'}")
                .append(",{ editable: true, name: '")
                .append(DBProperties.getProperty(EmployeeTimeCard.class.getName() + ".ExtraLaborTime"))
                .append("', field: 'ELT_").append(localDate.getDayOfYear()).append("'}")
                .append(",{ editable: true, name: '")
                .append(DBProperties.getProperty(EmployeeTimeCard.class.getName() + ".NightLaborTime"))
                .append("', field: 'NLT_").append(localDate.getDayOfYear()).append("'}")
                .append(",{ editable: true, name: '")
                .append(DBProperties.getProperty(EmployeeTimeCard.class.getName() + ".HolidayLaborTime"))
                .append("', field: 'HLT_").append(localDate.getDayOfYear()).append("'}");
        }
        script.append("")
                        .append("]")
                        .append("]")
                        .append("}")
                        .append("];")
                        .append("")
                        .append("");

        script.append("var data = {")
                        .append("identifier: \"oid\",")
                        .append(" items: [\n");
        ParameterUtil.setParmeterValue(_parameter, "selectedRow",
                        (String[]) Context.getThreadContext().getSessionAttribute(
                                        CIFormTimeReport.TimeReport_EmployeeTimeCardRegisterSelectForm.selection.name));
        final List<Instance> instances = getSelectedInstances(_parameter);
        final MultiPrintQuery multi = new MultiPrintQuery(instances);
        multi.setEnforceSorted(true);
        final SelectBuilder selEmpl = SelectBuilder.get().linkto(CITimeReport.EmployeeTimeCard.EmployeeLink);
        final SelectBuilder selEmplNumber = new SelectBuilder(selEmpl).attribute(CIHumanResource.EmployeeAbstract.Number);
        //HumanResource_EmployeeMsgPhrase
        final MsgPhrase emplPhrase = MsgPhrase.get(UUID.fromString("f543ca6d-29fb-4f1a-8747-0057b9a08404"));

        multi.addMsgPhrase(selEmpl, emplPhrase);

        multi.addSelect(selEmplNumber);
        multi.addAttribute(CITimeReport.EmployeeTimeCard.Name, CITimeReport.EmployeeTimeCard.Date,
                        CITimeReport.EmployeeTimeCard.DueDate);
        multi.execute();
        first = true;
        while (multi.next()) {
            final String name = multi.getAttribute(CITimeReport.EmployeeTimeCard.Name);
            final String number = multi.getSelect(selEmplNumber);
            final String employee = multi.getMsgPhrase(selEmpl, emplPhrase);

            final DateTime tcDate = multi.getAttribute(CITimeReport.EmployeeTimeCard.Date);
            final DateTime tcDueDate = multi.getAttribute(CITimeReport.EmployeeTimeCard.DueDate);
            final QueryBuilder queryBldr = new QueryBuilder(CITimeReport.EmployeeTimeCardPosition);
            queryBldr.addWhereAttrEqValue(CITimeReport.EmployeeTimeCardPosition.EmployeeTimeCardLink,
                            multi.getCurrentInstance());
            queryBldr.addWhereAttrGreaterValue(CITimeReport.EmployeeTimeCardPosition.Date, fromDate.minusDays(1));
            queryBldr.addWhereAttrLessValue(CITimeReport.EmployeeTimeCardPosition.Date, toDate.plusDays(1));
            final MultiPrintQuery posMulti = queryBldr.getPrint();
            posMulti.addAttribute(CITimeReport.EmployeeTimeCardPosition.Date,
                            CITimeReport.EmployeeTimeCardPosition.LaborTime,
                            CITimeReport.EmployeeTimeCardPosition.ExtraLaborTime,
                            CITimeReport.EmployeeTimeCardPosition.NightLaborTime,
                            CITimeReport.EmployeeTimeCardPosition.HolidayLaborTime);
            posMulti.execute();
            final Map<LocalDate, PosBean> map = new HashMap<>();
            while (posMulti.next()) {
                final PosBean bean = new PosBean();
                bean.setLaborTime((BigDecimal) posMulti.<Object[]>getAttribute(
                                CITimeReport.EmployeeTimeCardPosition.LaborTime)[0])
                                .setExtraLaborTime((BigDecimal) posMulti.<Object[]>getAttribute(
                                                CITimeReport.EmployeeTimeCardPosition.ExtraLaborTime)[0])
                                .setNightLaborTime((BigDecimal) posMulti.<Object[]>getAttribute(
                                                CITimeReport.EmployeeTimeCardPosition.NightLaborTime)[0])
                                .setHolidayLaborTime((BigDecimal) posMulti.<Object[]>getAttribute(
                                                CITimeReport.EmployeeTimeCardPosition.HolidayLaborTime)[0]);
                map.put(posMulti.<DateTime>getAttribute(CITimeReport.EmployeeTimeCardPosition.Date).toLocalDate(), bean);
            }

            if (first) {
                first = false;
            } else {
                script.append(",");
            }

            script.append("{").append("oid:\"").append(multi.getCurrentInstance().getOid())
                            .append("\", name:\"").append(name)
                            .append("\", number:\"").append(number)
                            .append("\", employee:\"").append(employee).append("\"");
            for (final LocalDate localDate : dates) {
                if (localDate.isEqual(tcDate.toLocalDate()) || localDate.isEqual(tcDueDate.toLocalDate())
                                || localDate.isAfter(tcDate.toLocalDate())
                                && localDate.isBefore(tcDueDate.toLocalDate())) {
                    PosBean bean;
                    if (map.containsKey(localDate)) {
                        bean = map.get(localDate);
                    } else {
                        bean = new PosBean();
                    }

                    script.append(",").append("LT_").append(localDate.getDayOfYear()).append(":")
                                    .append(bean.getLaborTime())
                                    .append(",").append("ELT_").append(localDate.getDayOfYear()).append(":")
                                    .append(bean.getExtraLaborTime())
                                    .append(",").append("NLT_").append(localDate.getDayOfYear()).append(":")
                                    .append(bean.getNightLaborTime())
                                    .append(",").append("HLT_").append(localDate.getDayOfYear()).append(":")
                                    .append(bean.getHolidayLaborTime());
                } else {
                    // table.addColumn("").getCurrentColumn().setCSSClass("firstC noneEdit")
                    // .getRow().addColumn("").getCurrentColumn().setCSSClass("noneEdit")
                    // .getRow().addColumn("").getCurrentColumn().setCSSClass("noneEdit")
                    // .getRow().addColumn("").getCurrentColumn().setCSSClass("noneEdit");
                }
            }
            script.append("}");
        }

        script.append("\n]")
                .append("};\n")
                .append("var store = new ItemFileWriteStore({data: data});\n")
                .append("var grid = new EnhancedGrid({")
                .append("id: 'grid',")
                .append("canSort: function(col){return false; },")
                .append("store: store,")
                .append("singleClickEdit: true,")
                .append("structure: layout,")
                .append("selectionMode: 'none'});\n")
                .append("grid.placeAt(\"gridDiv\");")
                .append("grid.startup();")
                .append("var gotList = function (items, request) {\n")
                .append("    var json = [\n")
                .append("    ];\n")
                .append("    dojo.forEach(items, function (i) {\n")
                .append("      json.push(itemToJSON(store, i));\n")
                .append("    });\n")
                .append("    var jsonStr = dojo.toJson(json);\n")
                .append("    document.getElementsByName('values')[0].value = jsonStr;\n")
                .append("  }\n ")
                .append("topic.subscribe(\"eFaps/submitClose\", function(){\n")
                .append(" store.fetch({\n")
                .append("onComplete: gotList\n")
                .append(" });\n")
                .append("});") ;

        final StringBuilder funScript = new StringBuilder()
                        .append("function itemToJSON(store, item) {\n"
                                        +
                                        "  var json = {\n"
                                        +
                                        "  };\n"
                                        +
                                        "  if (item && store) {\n"
                                        +
                                        "    // Determine the attributes we need to process.\n"
                                        +
                                        "    var attributes = store.getAttributes(item);\n"
                                        +
                                        "    if (attributes && attributes.length > 0) {\n"
                                        +
                                        "      var i;\n"
                                        +
                                        "      for (i = 0; i < attributes.length; i++) {\n"
                                        +
                                        "        var values = store.getValues(item, attributes[i]);\n"
                                        +
                                        "        if (values) {\n"
                                        +
                                        "          // Handle multivalued and single-valued attributes.\n"
                                        +
                                        "          if (values.length > 1) {\n"
                                        +
                                        "            var j;\n"
                                        +
                                        "            json[attributes[i]] = [\n"
                                        +
                                        "            ];\n"
                                        +
                                        "            for (j = 0; j < values.length; j++) {\n"
                                        +
                                        "              var value = values[j];\n"
                                        +
                                        "              // Check that the value isn't another item. If it is, process it as an item.\n"
                                        +
                                        "              if (store.isItem(value)) {\n"
                                        +
                                        "                json[attributes[i]].push(dojo.fromJson(itemToJSON(store, value)));\n"
                                        +
                                        "              } else {\n"
                                        +
                                        "                json[attributes[i]].push(value);\n"
                                        +
                                        "              }\n"
                                        +
                                        "            }\n"
                                        +
                                        "          } else {\n"
                                        +
                                        "            if (store.isItem(values[0])) {\n"
                                        +
                                        "              json[attributes[i]] = dojo.fromJson(itemToJSON(store, values[0]));\n"
                                        +
                                        "            } else {\n"
                                        +
                                        "              json[attributes[i]] = values[0];\n"
                                        +
                                        "            }\n"
                                        +
                                        "          }\n"
                                        +
                                        "        }\n"
                                        +
                                        "      }\n"
                                        +
                                        "    }\n"
                                        +
                                        "  }\n"
                                        +
                                        "  return json;\n"
                                        +
                                        "}\n");

        final StringBuilder html = new StringBuilder()
                        .append("<link rel=\"stylesheet\" type=\"text/css\" href=\"resource/org.efaps.ui.wicket.behaviors.dojo.AbstractDojoBehavior/dojox/grid/resources/Grid.css\" >")
                        .append("<link rel=\"stylesheet\" type=\"text/css\" href=\"resource/org.efaps.ui.wicket.behaviors.dojo.AbstractDojoBehavior/dojox/grid/resources/tundraGrid.css\" >")
                        .append("<style>")
                        .append(".dojoxGridCell.staticHeader {width: 120px;}")
                        .append("</style>")
                        .append("<div id=\"gridDiv\" style=\"position: absolute; height:98%; width: 90%;\">")
                        .append(InterfaceUtils.wrappInScriptTag(_parameter,
                                        funScript.append(InterfaceUtils.wrapInDojoRequire(_parameter, script,
                                        DojoLibs.ENHANCEDGRID, DojoLibs.IFWSTORE, DojoLibs.IFRSTORE, DojoLibs.FSELECT,
                                        DojoLibs.REGISTRY, DojoLibs.TOPIC)), true, 0));

        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    public Return getDateFieldvalue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String fieldName = getProperty(_parameter, "fieldName");
        ret.put(ReturnValues.VALUES, _parameter.getParameterValue(fieldName));
        return ret;
    }

    public Return register(final Parameter _parameter) throws EFapsException
    {
        final String jsonStr = _parameter.getParameterValue("values");
        if (jsonStr != null && !jsonStr.isEmpty()) {
            final DateTime fromDate = new DateTime(_parameter.getParameterValue(
                            CIFormTimeReport.TimeReport_EmployeeTimeCardRegisterEditForm.fromDate.name));
            final DateTime toDate = new DateTime(_parameter.getParameterValue(
                            CIFormTimeReport.TimeReport_EmployeeTimeCardRegisterEditForm.toDate.name));

            final ObjectMapper mapper = new ObjectMapper();
            try {
                final List<HashMap<String, Object>> values = mapper.readValue(jsonStr,
                                new TypeReference<List<HashMap<String, Object>>>()
                                {
                                });
                final Map<Instance, DataBean> beans = new HashMap<>();
                for (final HashMap<String, Object> map : values) {
                    final Instance instance = Instance.get(String.valueOf(map.get("oid")));
                    DataBean bean;
                    if (beans.containsKey(instance)) {
                        bean = beans.get(instance);
                    } else {
                        bean = new DataBean();
                        bean.setInstance(instance);
                        beans.put(instance, bean);
                    }
                    for (final Entry<String, Object> entry : map.entrySet()) {
                        bean.addPosition(entry.getKey(), entry.getValue());
                    }
                }
                for (final DataBean bean : beans.values()) {
                    bean.update(_parameter, fromDate, toDate);
                }
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return new Return();
    }

    public static class DataBean
    {

        private Instance instance;

        private Map<LocalDate, PosBean> beans = new TreeMap<>();

        /**
         * Getter method for the instance variable {@link #instance}.
         *
         * @return value of instance variable {@link #instance}
         */
        public Instance getInstance()
        {
            return this.instance;
        }

        /**
         * @param _parameter
         * @param _fromDate
         * @param _toDate
         */
        public void update(final Parameter _parameter,
                           final DateTime _fromDate,
                           final DateTime _toDate)
            throws EFapsException
        {
            final QueryBuilder queryBldr = new QueryBuilder(CITimeReport.EmployeeTimeCardPosition);
            queryBldr.addWhereAttrEqValue(CITimeReport.EmployeeTimeCardPosition.EmployeeTimeCardLink,
                            getInstance());
            queryBldr.addWhereAttrGreaterValue(CITimeReport.EmployeeTimeCardPosition.Date, _fromDate.minusDays(1));
            queryBldr.addWhereAttrLessValue(CITimeReport.EmployeeTimeCardPosition.Date, _toDate.plusDays(1));
            final InstanceQuery posInst = queryBldr.getQuery();
            final List<Instance> posInsts = posInst.execute();

            final Iterator<Instance> posIter = posInsts.iterator();
            for (final PosBean bean : this.beans.values()) {
                if (bean.isValid()) {
                    Update update;
                    if (posIter.hasNext()) {
                        update = new Update(posIter.next());
                    } else {
                        update = new Insert(CITimeReport.EmployeeTimeCardPosition);
                        final PrintQuery print = new PrintQuery(getInstance());
                        print.addAttribute(CITimeReport.EmployeeTimeCard.EmployeeLink);
                        print.execute();
                        update.add(CITimeReport.EmployeeTimeCardPosition.EmployeeLink,
                                        print.getAttribute(CITimeReport.EmployeeTimeCard.EmployeeLink));
                        update.add(CITimeReport.EmployeeTimeCardPosition.EmployeeTimeCardLink, getInstance());
                    }
                    update.add(CITimeReport.EmployeeTimeCardPosition.Date, new DateTime(bean.getDate().getYear(),
                                    bean.getDate().getMonthOfYear(), bean.getDate().getDayOfMonth(), 0, 0));
                    update.add(CITimeReport.EmployeeTimeCardPosition.LaborTime, bean.getLaborTimeObject());
                    update.add(CITimeReport.EmployeeTimeCardPosition.ExtraLaborTime, bean.getExtraLaborTimeObject());
                    update.add(CITimeReport.EmployeeTimeCardPosition.NightLaborTime, bean.getNightLaborTimeObject());
                    update.add(CITimeReport.EmployeeTimeCardPosition.HolidayLaborTime, bean.getHolidayLaborTimeObject());
                    update.execute();
                }
            }
            while (posIter.hasNext()) {
                new Delete(posIter.next()).execute();
            }
        }

        /**
         * @param _key
         * @param _value
         */
        public void addPosition(final String _key,
                                final Object _value)
        {
            final String[] keyArr = _key.split("_");
            if (keyArr != null && keyArr.length == 2) {
                final LocalDate date = new LocalDate().withDayOfYear(Integer.parseInt(keyArr[1]));
                PosBean bean;
                if (this.beans.containsKey(date)) {
                    bean = this.beans.get(date);
                } else {
                    bean = new PosBean();
                    bean.setDate(date);
                    this.beans.put(date, bean);
                }

                BigDecimal value = BigDecimal.ZERO;
                if (_value instanceof Number) {
                    value = new BigDecimal(((Number) _value).floatValue());
                }
                switch (keyArr[0]) {
                    case "LT":
                        bean.setLaborTime(value);
                        break;
                    case "ELT":
                        bean.setExtraLaborTime(value);
                        break;
                    case "NLT":
                        bean.setNightLaborTime(value);
                        break;
                    case "HLT":
                        bean.setHolidayLaborTime(value);
                        break;
                    default:
                        break;
                }
            }
        }

        /**
         * Setter method for instance variable {@link #instance}.
         *
         * @param _instance value for instance variable {@link #instance}
         */
        public void setInstance(final Instance _instance)
        {
            this.instance = _instance;
        }

        /**
         * Getter method for the instance variable {@link #beans}.
         *
         * @return value of instance variable {@link #beans}
         */
        public Map<LocalDate, PosBean> getBeans()
        {
            return this.beans;
        }

        /**
         * Setter method for instance variable {@link #beans}.
         *
         * @param _beans value for instance variable {@link #beans}
         */
        public void setBeans(final Map<LocalDate, PosBean> _beans)
        {
            this.beans = _beans;
        }
    }

    public static class PosBean
    {
        private LocalDate date;
        private BigDecimal LaborTime = BigDecimal.ZERO;
        private BigDecimal ExtraLaborTime = BigDecimal.ZERO;
        private BigDecimal NightLaborTime = BigDecimal.ZERO;
        private BigDecimal HolidayLaborTime = BigDecimal.ZERO;

        /**
         * Getter method for the instance variable {@link #laborTime}.
         *
         * @return value of instance variable {@link #laborTime}
         */
        public BigDecimal getLaborTime()
        {
            return this.LaborTime;
        }


        public Object[] getLaborTimeObject()
        {
            return new Object[] {
                            getLaborTime(),
                            CITimeReport.EmployeeTimeCardPosition.getType()
                                            .getAttribute(CITimeReport.EmployeeTimeCardPosition.LaborTime.name)
                                            .getDimension().getBaseUoM().getId() };
        }

        public Object[] getExtraLaborTimeObject()
        {
            return new Object[] {
                            getExtraLaborTime(),
                            CITimeReport.EmployeeTimeCardPosition.getType()
                                            .getAttribute(CITimeReport.EmployeeTimeCardPosition.ExtraLaborTime.name)
                                            .getDimension().getBaseUoM().getId() };
        }

        public Object[] getNightLaborTimeObject()
        {
            return new Object[] {
                            getNightLaborTime(),
                            CITimeReport.EmployeeTimeCardPosition.getType()
                                            .getAttribute(CITimeReport.EmployeeTimeCardPosition.NightLaborTime.name)
                                            .getDimension().getBaseUoM().getId() };
        }

        public Object[] getHolidayLaborTimeObject()
        {
            return new Object[] {
                            getHolidayLaborTime(),
                            CITimeReport.EmployeeTimeCardPosition.getType()
                                            .getAttribute(CITimeReport.EmployeeTimeCardPosition.HolidayLaborTime.name)
                                            .getDimension().getBaseUoM().getId() };
        }

        /**
         * Setter method for instance variable {@link #laborTime}.
         *
         * @param _laborTime value for instance variable {@link #laborTime}
         */
        public PosBean setLaborTime(final BigDecimal _laborTime)
        {
            this.LaborTime = _laborTime;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #extraLaborTime}.
         *
         * @return value of instance variable {@link #extraLaborTime}
         */
        public BigDecimal getExtraLaborTime()
        {
            return this.ExtraLaborTime;
        }

        /**
         * Setter method for instance variable {@link #extraLaborTime}.
         *
         * @param _extraLaborTime value for instance variable
         *            {@link #extraLaborTime}
         */
        public PosBean setExtraLaborTime(final BigDecimal _extraLaborTime)
        {
            this.ExtraLaborTime = _extraLaborTime;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #nightLaborTime}.
         *
         * @return value of instance variable {@link #nightLaborTime}
         */
        public BigDecimal getNightLaborTime()
        {
            return this.NightLaborTime;
        }

        /**
         * Setter method for instance variable {@link #nightLaborTime}.
         *
         * @param _nightLaborTime value for instance variable
         *            {@link #nightLaborTime}
         */
        public PosBean setNightLaborTime(final BigDecimal _nightLaborTime)
        {
            this.NightLaborTime = _nightLaborTime;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #holidayLaborTime}.
         *
         * @return value of instance variable {@link #holidayLaborTime}
         */
        public BigDecimal getHolidayLaborTime()
        {
            return this.HolidayLaborTime;
        }

        /**
         * Setter method for instance variable {@link #holidayLaborTime}.
         *
         * @param _holidayLaborTime value for instance variable
         *            {@link #holidayLaborTime}
         */
        public PosBean setHolidayLaborTime(final BigDecimal _holidayLaborTime)
        {
            this.HolidayLaborTime = _holidayLaborTime;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public LocalDate getDate()
        {
            return this.date;
        }

        /**
         * Setter method for instance variable {@link #date}.
         *
         * @param _date value for instance variable {@link #date}
         */
        public void setDate(final LocalDate _date)
        {
            this.date = _date;
        }

        public boolean isValid()
        {
            return getLaborTime().compareTo(BigDecimal.ZERO) > 0
                            ||  getExtraLaborTime().compareTo(BigDecimal.ZERO) > 0
                            ||  getNightLaborTime().compareTo(BigDecimal.ZERO) > 0
                            ||  getHolidayLaborTime().compareTo(BigDecimal.ZERO) > 0;
        }
    }
}
