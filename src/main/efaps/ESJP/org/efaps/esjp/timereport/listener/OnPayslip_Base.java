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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CITimeReport;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uiform.Field_Base;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.CommonDocument_Base.CreatedDoc;
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
    extends AbstractOnPayroll
    implements IOnPayslip
{

    private Instance payslipInst;

    @Override
    public BigDecimal getLaborTime(final Parameter _parameter,
                                   final Instance _payslipInst,
                                   final DateTime _date,
                                   final DateTime _dueDate,
                                   final Instance _emplInst)
        throws EFapsException
    {
        setDate(_date);
        setDueDate(_dueDate);
        setEmplInst(_emplInst);
        setPayslipInst(_payslipInst);
        final TimeBean bean = getTimeBean(_parameter);
        return bean.getLaborTime();
    }

    @Override
    public BigDecimal getExtraLaborTime(final Parameter _parameter,
                                        final Instance _payslipInst,
                                        final DateTime _date,
                                        final DateTime _dueDate,
                                        final Instance _emplInst)
        throws EFapsException
    {
        setDate(_date);
        setDueDate(_dueDate);
        setEmplInst(_emplInst);
        setPayslipInst(_payslipInst);
        final TimeBean bean = getTimeBean(_parameter);
        return bean.getExtraLaborTime();
    }

    @Override
    public BigDecimal getNightLaborTime(final Parameter _parameter,
                                        final Instance _payslipInst,
                                        final DateTime _date,
                                        final DateTime _dueDate,
                                        final Instance _emplInst)
        throws EFapsException
    {
        setDate(_date);
        setDueDate(_dueDate);
        setEmplInst(_emplInst);
        setPayslipInst(_payslipInst);
        final TimeBean bean = getTimeBean(_parameter);
        return bean.getNightLaborTime();
    }

    @Override
    public BigDecimal getHolidayLaborTime(final Parameter _parameter,
                                          final Instance _payslipInst,
                                          final DateTime _date,
                                          final DateTime _dueDate,
                                          final Instance _emplInst)
        throws EFapsException
    {
        setDate(_date);
        setDueDate(_dueDate);
        setEmplInst(_emplInst);
        setPayslipInst(_payslipInst);
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
                                 final QueryBuilder _queryBldr)
        throws EFapsException
    {
        if (getPayslipInst() == null) {
            final String[] oids = _parameter.getParameterValues(CITimeReport.EmployeeTimeCard.getType().getName());
            if (oids != null) {
                final List<Instance> instances = new ArrayList<>();
                for (final String oid : oids) {
                    final Instance instance = Instance.get(oid);
                    if (instance.isValid()) {
                        instances.add(instance);
                    }
                }
                _queryBldr.addWhereAttrEqValue(CITimeReport.EmployeeTimeCardPosition.DocumentAbstractLink,
                                instances.toArray());
            } else {
                _queryBldr.addWhereAttrEqValue(CITimeReport.EmployeeTimeCardPosition.DocumentAbstractLink, -1);
            }

        } else {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CITimeReport.EmployeeTimeCard2Payslip);
            attrQueryBldr.addWhereAttrEqValue(CITimeReport.EmployeeTimeCard2Payslip.ToLink, getPayslipInst());
            _queryBldr.addWhereAttrInQuery(CITimeReport.EmployeeTimeCardPosition.DocumentAbstractLink,
                            attrQueryBldr.getAttributeQuery(CITimeReport.EmployeeTimeCard2Payslip.FromLink));
        }
        _queryBldr.addWhereAttrEqValue(CITimeReport.EmployeeTimeCardPosition.EmployeeLink, getEmplInst());
    }

    @Override
    public void add2UpdateMap4Employee(final Parameter _parameter,
                                       final Instance _emplInst,
                                       final Map<String, Object> _map)
        throws EFapsException
    {
        final List<DropDownPosition> values = new ArrayList<>();

        @SuppressWarnings("unchecked")
        final MultiPrintQuery multi = new MultiPrintQuery((List<Instance>) getEmployeeTimeCardInst(_parameter,
                        _emplInst));
        multi.addAttribute(CITimeReport.EmployeeTimeCard.Name);
        multi.execute();
        while (multi.next()) {
            values.add(new DropDownPosition(multi.getCurrentInstance().getOid(), multi.getCurrentInstance().getType()
                            .getLabel() + " "
                            + multi.getAttribute(CITimeReport.EmployeeTimeCard.Name)).setSelected(true));
        }
        if (!values.isEmpty()) {
            final Parameter parameter = ParameterUtil.clone(_parameter);
            ParameterUtil.setProperty(parameter, "FieldName", CITimeReport.EmployeeTimeCard.getType().getName());
            final StringBuilder html = new Field().getInputField(parameter, values, Field_Base.ListType.CHECKBOX);
            final StringBuilder js = new StringBuilder();
            js.append("document.getElementsByName('").append(CIFormPayroll.Payroll_PayslipForm.additionalInfo.name)
                            .append("')[0].innerHTML=\"").append(StringEscapeUtils.escapeEcmaScript(html.toString()))
                            .append("\";");
            InterfaceUtils.appendScript4FieldUpdate(_map, js);
        }
    }

    /**
     * Getter method for the instance variable {@link #payslipInst}.
     *
     * @return value of instance variable {@link #payslipInst}
     */
    public Instance getPayslipInst()
    {
        return this.payslipInst;
    }

    /**
     * Setter method for instance variable {@link #payslipInst}.
     *
     * @param _payslipInst value for instance variable {@link #payslipInst}
     */
    public void setPayslipInst(final Instance _payslipInst)
    {
        this.payslipInst = _payslipInst;
    }

    @Override
    public void afterCreate(final Parameter _parameter,
                            final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final List<Instance> instances = getInstances(_parameter, CITimeReport.EmployeeTimeCard.getType().getName());
        for (final Instance instance : instances) {
            final Insert insert = new Insert(CITimeReport.EmployeeTimeCard2Payslip);
            insert.add(CITimeReport.EmployeeTimeCard2Payslip.FromLink, instance);
            insert.add(CITimeReport.EmployeeTimeCard2Payslip.ToLink, _createdDoc.getInstance());
            insert.execute();
        }
    }

    @Override
    public Collection<? extends Instance> getEmployeeTimeCardInst(final Parameter _parameter,
                                                                  final Instance _emplInst)
        throws EFapsException
    {
        final QueryBuilder attrQueryBldr = new QueryBuilder(CITimeReport.EmployeeTimeCard2Payslip);

        final QueryBuilder queryBldr = new QueryBuilder(CITimeReport.EmployeeTimeCard);
        queryBldr.addWhereAttrNotEqValue(CITimeReport.EmployeeTimeCard.Status,
                        Status.find(CITimeReport.EmployeeTimeCardStatus.Canceled));
        queryBldr.addWhereAttrEqValue(CITimeReport.EmployeeTimeCard.EmployeeLink, _emplInst);
        queryBldr.addWhereAttrNotInQuery(CITimeReport.EmployeeTimeCard.ID,
                        attrQueryBldr.getAttributeQuery(CITimeReport.EmployeeTimeCard2Payslip.FromLink));
        return queryBldr.getQuery().execute();
    }
}
