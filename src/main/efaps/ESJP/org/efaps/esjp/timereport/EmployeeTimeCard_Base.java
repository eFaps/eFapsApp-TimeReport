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
import org.efaps.db.CachedMultiPrintQuery;
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
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormTimeReport;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ci.CITimeReport;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.common.uitable.MultiPrint;
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
        final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("E dd/MM/YY").withLocale(
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
            .append("var selectStore = new Memory({\n")
            .append("data: {\n")
            .append("items: [\n")
            .append("{ id: ").append(0)
            .append(",label: '").append("-")
            .append("'}\n");
        final QueryBuilder rQueryBldr = new QueryBuilder(CITimeReport.AttributeDefinitionAbsenceReason);
        rQueryBldr.addWhereAttrEqValue(CITimeReport.AttributeDefinitionAbsenceReason.Status,
                        Status.find(CIERP.AttributeDefinitionStatus.Active));
        final MultiPrintQuery rMutli = rQueryBldr.getPrint();
        rMutli.addAttribute(CITimeReport.AttributeDefinitionAbsenceReason.Value);
        rMutli.execute();

        while (rMutli.next()) {
            script.append(",{ id: ").append(rMutli.getCurrentInstance().getId())
                .append(",label: '").append(rMutli.getAttribute(CITimeReport.AttributeDefinitionAbsenceReason.Value))
                .append("'}\n");
        }

        script.append("]\n")
            .append("}\n")
            .append("})\n")
            .append("\n")
            .append("var selectCreated = function (editor, column) {\n")
            .append("editor.setStore(selectStore);\n")
            .append("}\n")

            .append("var numCreated = function (editor, column) {\n")
            .append("domAttr.set(editor.textbox, 'type', 'number');\n")
            .append("domAttr.set(editor.textbox, 'min', '0');\n")
            .append("domAttr.set(editor.textbox, 'max', '12');\n")
            .append("}\n")
            .append("var isEditable = function (cell) {\n")
            .append("return cell.rawData()!= undefined ;\n")
            .append("}\n")
            .append("var layout = [\n")
            .append("{ field: 'name', name: 'No', width: '50px'},\n")
            .append("{ field: 'number', name: 'DNI', width: '50px'},\n")
            .append("{ field: 'employee', name: 'Nombre', width: '200px'}\n");

        for (final LocalDate localDate : dates) {
            script.append(",")
                .append("{ editable: true, editor: Select,")
                .append("editorArgs: { props: 'labelAttr: \"label\", style: {width: \"100%\"}'},\n")
                .append("formatter: function (itemData, rowIndex, rowId) {\n")
                .append("var d = itemData['A_").append(localDate.getDayOfYear()).append("'];")
                .append("return d == undefined ? \"\" : selectStore.get(d).label ;\n")
                .append("},\n")
                .append(" onEditorCreated : selectCreated, canEdit: isEditable,  headerClass: ' dow")
                .append(localDate.getDayOfWeek()).append("',")
                .append(" width: '50px', name: '")
                .append(DBProperties.getProperty(EmployeeTimeCard.class.getName() + ".absenceType"))
                .append("', field: 'A_").append(localDate.getDayOfYear()).append("'}\n")
                .append(",{ editable: true, onEditorCreated : numCreated, ")
                .append(" headerClass: ' dow").append(localDate.getDayOfWeek()).append("',")
                .append(" canEdit: isEditable,   width: '30px', name: '")
                .append(DBProperties.getProperty(EmployeeTimeCard.class.getName() + ".LaborTime"))
                .append("', field: 'LT_").append(localDate.getDayOfYear()).append("'}\n")
                .append(",{ editable: true, onEditorCreated : numCreated, ")
                .append(" headerClass: ' dow").append(localDate.getDayOfWeek()).append("',")
                .append(" canEdit: isEditable,   width: '30px', name: '")
                .append(DBProperties.getProperty(EmployeeTimeCard.class.getName() + ".ExtraLaborTime"))
                .append("', field: 'ELT_").append(localDate.getDayOfYear()).append("'}\n")
                .append(",{  editable: true, onEditorCreated : numCreated, ")
                .append(" headerClass: ' dow").append(localDate.getDayOfWeek()).append("',")
                .append(" canEdit: isEditable,   width: '30px', name: '")
                .append(DBProperties.getProperty(EmployeeTimeCard.class.getName() + ".NightLaborTime"))
                .append("', field: 'NLT_").append(localDate.getDayOfYear()).append("'}\n")
                .append(",{  editable: true, onEditorCreated : numCreated, ")
                .append(" headerClass: ' dow").append(localDate.getDayOfWeek()).append("',")
                .append(" canEdit: isEditable,  width: '30px', name: '")
                .append(DBProperties.getProperty(EmployeeTimeCard.class.getName() + ".HolidayLaborTime"))
                .append("', field: 'HLT_").append(localDate.getDayOfYear()).append("'}\n");
        }

        script.append("];\n")
            .append(" docHeaderGroup = [\n")
            .append("{name: 'Empleado', children: 3} \n");

        for (final LocalDate localDate : dates) {
            script.append(",{ children: 5, name: '").append(localDate.toString(dateFormatter)).append("'}");
        }

        script.append("];\n")
            .append("var data = {")
            .append("identifier: \"oid\",")
            .append(" items: [\n");
        ParameterUtil.setParmeterValue(_parameter, "selectedRow",
                        (String[]) Context.getThreadContext().getSessionAttribute(
                                        CIFormTimeReport.TimeReport_EmployeeTimeCardRegisterSelectForm.selection.name));
        final List<Instance> instances = getSelectedInstances(_parameter);
        final MultiPrintQuery multi = new MultiPrintQuery(instances);
        multi.setEnforceSorted(true);
        final SelectBuilder selEmpl = SelectBuilder.get().linkto(CITimeReport.EmployeeTimeCard.EmployeeLink);
        final SelectBuilder selEmplInst = new SelectBuilder(selEmpl).instance();
        final SelectBuilder selEmplStatus = new SelectBuilder(selEmpl).status();
        final SelectBuilder selEmplNumber = new SelectBuilder(selEmpl).attribute(
                        CIHumanResource.EmployeeAbstract.Number);
        //HumanResource_EmployeeMsgPhrase
        final MsgPhrase emplPhrase = MsgPhrase.get(UUID.fromString("f543ca6d-29fb-4f1a-8747-0057b9a08404"));
        multi.addMsgPhrase(selEmpl, emplPhrase);
        multi.addSelect(selEmplInst, selEmplNumber, selEmplStatus);
        multi.addAttribute(CITimeReport.EmployeeTimeCard.Name, CITimeReport.EmployeeTimeCard.Date,
                        CITimeReport.EmployeeTimeCard.DueDate);
        multi.execute();
        boolean first = true;
        while (multi.next()) {
            final String name = multi.getAttribute(CITimeReport.EmployeeTimeCard.Name);
            final String number = multi.getSelect(selEmplNumber);
            final String employee = multi.getMsgPhrase(selEmpl, emplPhrase);
            final Status emplStatus = multi.getSelect(selEmplStatus);

            if (emplStatus.equals(Status.find(CIHumanResource.EmployeeStatus.Worker))) {
                final DateTime tcDate = multi.getAttribute(CITimeReport.EmployeeTimeCard.Date);
                final DateTime tcDueDate = multi.getAttribute(CITimeReport.EmployeeTimeCard.DueDate);
                final QueryBuilder queryBldr = new QueryBuilder(CITimeReport.EmployeeTimeCardPosition);
                queryBldr.addType(CITimeReport.EmployeeAbsencePosition);
                queryBldr.addWhereAttrEqValue(CITimeReport.EmployeeTimeCardPosition.DocumentAbstractLink,
                                multi.getCurrentInstance());
                queryBldr.addWhereAttrGreaterValue(CITimeReport.EmployeeTimeCardPosition.Date, fromDate.minusDays(1));
                queryBldr.addWhereAttrLessValue(CITimeReport.EmployeeTimeCardPosition.Date, toDate.plusDays(1));
                final MultiPrintQuery posMulti = queryBldr.getPrint();
                posMulti.addAttribute(CITimeReport.EmployeeTimeCardPosition.AttrDefLinkAbstract,
                                CITimeReport.EmployeeTimeCardPosition.Date,
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
                                                    CITimeReport.EmployeeTimeCardPosition.HolidayLaborTime)[0])
                        .setReasonID(posMulti.<Long>getAttribute(
                                        CITimeReport.EmployeeTimeCardPosition.AttrDefLinkAbstract));
                    map.put(posMulti.<DateTime>getAttribute(
                                    CITimeReport.EmployeeTimeCardPosition.Date).toLocalDate(), bean);
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
                            bean = eval4Bean(_parameter, multi.<Instance>getSelect(selEmplInst), localDate);
                        }
                        script.append(",").append("A_").append(localDate.getDayOfYear()).append(":")
                            .append(bean.getReasonID())
                            .append(",").append("LT_").append(localDate.getDayOfYear()).append(":")
                            .append(bean.getLaborTime())
                            .append(",").append("ELT_").append(localDate.getDayOfYear()).append(":")
                            .append(bean.getExtraLaborTime())
                            .append(",").append("NLT_").append(localDate.getDayOfYear()).append(":")
                            .append(bean.getNightLaborTime())
                            .append(",").append("HLT_").append(localDate.getDayOfYear()).append(":")
                            .append(bean.getHolidayLaborTime());
                    }
                }
                script.append("}");
            }
        }

        script.append("\n]")
            .append("};\n")
            .append("var store = new Memory({data: data});\n")

            .append("var grid = new Grid({")
            .append("id: 'grid',")
            .append("store: store,")
            .append("structure: layout,")
            .append("cacheClass: Cache,")
            .append("columnLockCount: 3,")
            .append("headerGroups: docHeaderGroup,")
            .append("modules: [\n")
            .append("GroupHeader,\n")
            .append("ColumnLock,\n")
            .append("Edit,\n")
            .append("CellWidget\n")
            .append("]\n")

            .append("});\n")
            .append("grid.placeAt(\"gridDiv\");")
            .append("grid.edit.foc = grid.edit._focus;\n")
            .append("grid.edit._focus= function(rowId, colId) {\n")
            .append("grid.edit.foc(rowId, colId);\n")
            .append(" var editor = grid.edit.getEditor(rowId, colId);\n")
            .append("if (editor.textbox) {\n")
            .append("editor.focusNode.select();\n")
            .append("}\n")
            .append("};\n")

            .append("grid.startup();")
            .append("topic.subscribe(\"eFaps/submitClose\", function(){\n")

            .append("registry.byId('grid').model.save();\n")
            .append("var json = [];\n")
            .append("var query = store.query({ oid: /\\w*/ }).forEach(function (item) {\n")
            .append("json.push(item);\n")
            .append("});\n")
            .append("var jsonStr = dojo.toJson(json);\n")
            .append("document.getElementsByName('values') [0].value = jsonStr;\n")
            .append("});\n");

        final StringBuilder html = new StringBuilder()
            .append("<link rel=\"stylesheet\" type=\"text/css\" ")
            .append("href=\"resource/org.efaps.ui.wicket.behaviors.dojo.AbstractDojoBehavior")
                .append("/gridx/resources/claro/Gridx.css\" >")
            .append("<link rel=\"stylesheet\" type=\"text/css\" ")
            .append("href=\"resource/org.efaps.ui.wicket.behaviors.dojo.AbstractDojoBehavior")
                .append("/dojox/grid/resources/tundraGrid.css\" >")
            .append("<style>")
            .append(".dojoxGridCell.staticHeader {width: 120px;}")
            .append(".dow7 {background-color: #CC8181;}")
            .append("</style>")
            .append("<div id=\"gridDiv\" style=\"position: absolute; height:98%; width: 98%;\">")
            .append(InterfaceUtils.wrappInScriptTag(_parameter,
                                        InterfaceUtils.wrapInDojoRequire(_parameter, script,
                                        DojoLibs.MEMORY , DojoLibs.GXGRID, DojoLibs.GXCACHE, DojoLibs.GXGRPHEADER,
                                        DojoLibs.GXEDIT, DojoLibs.CLMLOCK, DojoLibs.GXEDIT, DojoLibs.GXCELLWIDGET,
                                        DojoLibs.SELECT, DojoLibs.NBRTEXTBOX, DojoLibs.DOMATTR,
                                        DojoLibs.REGISTRY, DojoLibs.TOPIC, DojoLibs.LANG), true, 0));

        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    protected PosBean eval4Bean(final Parameter _parameter,
                                final Instance _emplInst,
                                final LocalDate _localDate)
        throws EFapsException
    {
        final PosBean ret = new PosBean();

        final QueryBuilder attQueryBldr = new QueryBuilder(CITimeReport.EmployeePreplannedAbsence);
        attQueryBldr.addWhereAttrEqValue(CITimeReport.EmployeePreplannedAbsence.EmployeeAbstractLink, _emplInst);
        attQueryBldr.addWhereAttrEqValue(CITimeReport.EmployeePreplannedAbsence.Status,
                        Status.find(CITimeReport.EmployeePreplannedAbsenceStatus.Closed));
        final QueryBuilder queryBldr = new QueryBuilder(CITimeReport.EmployeePreplannedAbsencePosition);
        queryBldr.addWhereAttrInQuery(CITimeReport.EmployeePreplannedAbsencePosition.DocumentAbstractLink,
                        attQueryBldr.getAttributeQuery(CITimeReport.EmployeePreplannedAbsence.ID));
        queryBldr.addWhereAttrLessValue(CITimeReport.EmployeePreplannedAbsencePosition.FromDate,
                        _localDate.toDateTimeAtStartOfDay().plusMinutes(1));
        queryBldr.addWhereAttrGreaterValue(CITimeReport.EmployeePreplannedAbsencePosition.ToDate,
                        _localDate.toDateTimeAtStartOfDay().minusMinutes(1));
        final CachedMultiPrintQuery multi = queryBldr.getCachedPrint4Request();
        multi.addAttribute(CITimeReport.EmployeePreplannedAbsencePosition.AbsenceReasonLink);
        multi.execute();
        while (multi.next()) {
            ret.setReasonID(multi.<Long>getAttribute(CITimeReport.EmployeePreplannedAbsencePosition.AbsenceReasonLink));
        }
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

    public Return register(final Parameter _parameter)
        throws EFapsException
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
                                new TypeReference<List<HashMap<String, Object>>>() { });
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

    public Return multiPrint4Project(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                super.add2QueryBldr(_parameter, _queryBldr);
                final QueryBuilder attrQueryBldr = new QueryBuilder(
                                CITimeReport.Projects_ProjectService2EmployeeTimeCard);
                attrQueryBldr.addWhereAttrEqValue(CITimeReport.Projects_ProjectService2EmployeeTimeCard.FromLink,
                                _parameter.getInstance());

                _queryBldr.addWhereAttrInQuery(CITimeReport.DocumentAbstract.ID,
                                attrQueryBldr.getAttributeQuery(
                                                CITimeReport.Projects_ProjectService2EmployeeTimeCard.ToAbstract));
            }
        };
        return multi.execute(_parameter);
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
            queryBldr.addType(CITimeReport.EmployeeAbsencePosition);
            queryBldr.addWhereAttrEqValue(CITimeReport.EmployeeAbstractPosition.DocumentAbstractLink,
                            getInstance());
            queryBldr.addWhereAttrGreaterValue(CITimeReport.EmployeeAbstractPosition.Date, _fromDate.minusDays(1));
            queryBldr.addWhereAttrLessValue(CITimeReport.EmployeeAbstractPosition.Date, _toDate.plusDays(1));
            final InstanceQuery posInst = queryBldr.getQuery();
            final List<Instance> posInsts = posInst.execute();
            final List<Instance> toDelete = new ArrayList<>();
            final Iterator<Instance> posIter = posInsts.iterator();
            for (final PosBean bean : this.beans.values()) {
                if (bean.isValid()) {
                    Update update = null;
                    if (posIter.hasNext()) {
                        final Instance currInst = posIter.next();
                        if (bean.getReasonID() > 0 && currInst.getType()
                                        .isCIType(CITimeReport.EmployeeAbsencePosition)
                                        || bean.getReasonID() == 0 && currInst.getType().isCIType(
                                                        CITimeReport.EmployeeTimeCardPosition)) {
                            update = new Update(currInst);
                        } else {
                            toDelete.add(currInst);
                        }
                    }

                    if (update == null) {
                        update = new Insert(bean.getReasonID() > 0 ? CITimeReport.EmployeeAbsencePosition
                                        : CITimeReport.EmployeeTimeCardPosition);
                        final PrintQuery print = new PrintQuery(getInstance());
                        print.addAttribute(CITimeReport.EmployeeTimeCard.EmployeeLink);
                        print.execute();
                        update.add(CITimeReport.EmployeeAbstractPosition.EmployeeAbstractLink,
                                        print.getAttribute(CITimeReport.EmployeeTimeCard.EmployeeLink));
                        update.add(CITimeReport.EmployeeAbstractPosition.DocumentAbstractLink, getInstance());
                    }
                    update.add(CITimeReport.EmployeeAbstractPosition.Date, new DateTime(bean.getDate().getYear(),
                                    bean.getDate().getMonthOfYear(), bean.getDate().getDayOfMonth(), 0, 0));
                    update.add(CITimeReport.EmployeeAbstractPosition.AttrDefLinkAbstract, bean.getReasonObject());
                    update.add(CITimeReport.EmployeeAbstractPosition.LaborTime, bean.getLaborTimeObject());
                    update.add(CITimeReport.EmployeeAbstractPosition.ExtraLaborTime, bean.getExtraLaborTimeObject());
                    update.add(CITimeReport.EmployeeAbstractPosition.NightLaborTime, bean.getNightLaborTimeObject());
                    update.add(CITimeReport.EmployeeAbstractPosition.HolidayLaborTime, bean.getHolidayLaborTimeObject());
                    update.execute();
                }
            }
            while (posIter.hasNext()) {
                toDelete.add(posIter.next());
            }
            for (final Instance inst: toDelete) {
                new Delete(inst).execute();
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
                } else if (_value instanceof String) {
                    value = new BigDecimal((String) _value);
                }
                switch (keyArr[0]) {
                    case "A":
                        bean.setReasonID(value.longValue());
                        break;
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

        private Long reasonID = Long.valueOf(0);

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
            Object[] ret = null;
            if (this.reasonID == 0) {
                ret = new Object[] {
                                getLaborTime(),
                                CITimeReport.EmployeeTimeCardPosition.getType()
                                                .getAttribute(CITimeReport.EmployeeTimeCardPosition.LaborTime.name)
                                                .getDimension().getBaseUoM().getId() };
            }
            return ret;
        }

        public Object[] getExtraLaborTimeObject()
        {
            Object[] ret = null;
            if (this.reasonID == 0) {
                ret = new Object[] {
                                getExtraLaborTime(),
                                CITimeReport.EmployeeTimeCardPosition
                                                .getType()
                                                .getAttribute(CITimeReport.EmployeeTimeCardPosition.ExtraLaborTime.name)
                                                .getDimension().getBaseUoM().getId() };
            }
            return ret;
        }

        public Object[] getNightLaborTimeObject()
        {
            Object[] ret = null;
            if (this.reasonID == 0) {
                ret = new Object[] {
                                getNightLaborTime(),
                                CITimeReport.EmployeeTimeCardPosition
                                                .getType()
                                                .getAttribute(CITimeReport.EmployeeTimeCardPosition.NightLaborTime.name)
                                                .getDimension().getBaseUoM().getId() };
            }
            return ret;
        }

        public Object[] getHolidayLaborTimeObject()
        {
            Object[] ret = null;
            if (this.reasonID == 0) {
                ret = new Object[] {
                                getHolidayLaborTime(),
                                CITimeReport.EmployeeTimeCardPosition
                                            .getType()
                                            .getAttribute(CITimeReport.EmployeeTimeCardPosition.HolidayLaborTime.name)
                                            .getDimension().getBaseUoM().getId() };
            }
            return ret;
        }

        /**
         * Setter method for instance variable {@link #laborTime}.
         *
         * @param _laborTime value for instance variable {@link #laborTime}
         * @return this for chaining
         */
        public PosBean setLaborTime(final BigDecimal _laborTime)
        {
            if (_laborTime != null) {
                this.LaborTime = _laborTime;
            }
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
         * @return this for chaining
         */
        public PosBean setExtraLaborTime(final BigDecimal _extraLaborTime)
        {
            if (_extraLaborTime != null) {
                this.ExtraLaborTime = _extraLaborTime;
            }
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
         * @return this for chaining
         */
        public PosBean setNightLaborTime(final BigDecimal _nightLaborTime)
        {
            if (_nightLaborTime != null) {
                this.NightLaborTime = _nightLaborTime;
            }
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
         * @param _holidayLaborTime value for instance variable {@link #holidayLaborTime}
         * @return this for chaining
         */
        public PosBean setHolidayLaborTime(final BigDecimal _holidayLaborTime)
        {
            if (_holidayLaborTime != null) {
                this.HolidayLaborTime = _holidayLaborTime;
            }
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
                            ||  getHolidayLaborTime().compareTo(BigDecimal.ZERO) > 0
                            ||  getReasonID() > 0;
        }

        /**
         * Getter method for the instance variable {@link #reasonID}.
         *
         * @return value of instance variable {@link #reasonID}
         */
        public Long getReasonID()
        {
            return this.reasonID;
        }

        /**
         * Getter method for the instance variable {@link #reasonID}.
         *
         * @return value of instance variable {@link #reasonID}
         */
        public Long getReasonObject()
        {
            Long ret = null;
            if (this.reasonID > 0) {
                ret = this.reasonID;
            }
            return ret;
        }

        /**
         * Setter method for instance variable {@link #reasonID}.
         *
         * @param _reasonID value for instance variable {@link #reasonID}
         */
        public void setReasonID(final Long _reasonID)
        {
            if (_reasonID != null) {
                this.reasonID = _reasonID;
            }
        }
    }
}
