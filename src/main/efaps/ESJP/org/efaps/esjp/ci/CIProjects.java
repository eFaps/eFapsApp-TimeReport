//CHECKSTYLE:OFF
package org.efaps.esjp.ci;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsNoUpdate;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;
import org.efaps.ci.CIStatus;
import org.efaps.ci.CIType;

/**
 * This class is only used in case that the Projects App is not installed to be
 * able to compile the classes.
 *
 * @author The eFaps Team
 */
@EFapsUUID("83c21d9f-8108-4eb2-b989-981ee22c5c55")
@EFapsApplication("eFapsApp-TimeReport")
@EFapsNoUpdate
public final class CIProjects
{

    public static final _Project2DocumentAbstract Project2DocumentAbstract = new _Project2DocumentAbstract(
                    "a6accf51-06d0-4882-a4c7-617cd5bf789b");

    public static class _Project2DocumentAbstract
        extends CIType
    {

        protected _Project2DocumentAbstract(final String _uuid)
        {
            super(_uuid);
        }

        public final CIAttribute Created = new CIAttribute(this, "Created");
        public final CIAttribute Creator = new CIAttribute(this, "Creator");
        public final CIAttribute FromAbstract = new CIAttribute(this, "FromAbstract");
        public final CIAttribute Modified = new CIAttribute(this, "Modified");
        public final CIAttribute Modifier = new CIAttribute(this, "Modifier");
        public final CIAttribute ToAbstract = new CIAttribute(this, "ToAbstract");
    }

    public static final _ProjectService2DocumentAbstract ProjectService2DocumentAbstract = new _ProjectService2DocumentAbstract(
                    "bcb41bad-a349-4012-9cb8-2afd16830aa3");

    public static class _ProjectService2DocumentAbstract
        extends _Project2DocumentAbstract
    {

        protected _ProjectService2DocumentAbstract(final String _uuid)
        {
            super(_uuid);
        }

        public final CIAttribute FromService = new CIAttribute(this, "FromService");
        public final CIAttribute ToDocument = new CIAttribute(this, "ToDocument");
    }

    public static final _ProjectService2Employee ProjectService2Employee = new _ProjectService2Employee(
                    "967e7a7a-2f7f-4462-aead-e8633e44b871");

    public static class _ProjectService2Employee
        extends org.efaps.esjp.ci.CIERP._Object2ObjectAbstract
    {

        protected _ProjectService2Employee(final String _uuid)
        {
            super(_uuid);
        }

        public final CIAttribute FromLink = new CIAttribute(this, "FromLink");
        public final CIAttribute Status = new CIAttribute(this, "Status");
        public final CIAttribute ToLink = new CIAttribute(this, "ToLink");
    }

    public static final _ProjectService2EmployeeStatus ProjectService2EmployeeStatus = new _ProjectService2EmployeeStatus(
                    "3b4f6743-1b59-4d1f-8c98-4a0f1e44c8c1");

    public static class _ProjectService2EmployeeStatus
        extends org.efaps.esjp.ci.CIAdmin._DataModel_StatusAbstract
    {

        protected _ProjectService2EmployeeStatus(final String _uuid)
        {
            super(_uuid);
        }

        public final CIStatus Active = new CIStatus(this, "Active");
        public final CIStatus Inactive = new CIStatus(this, "Inactive");
    }

    public static final _ProjectAbstract ProjectAbstract = new _ProjectAbstract("5d76d1ca-a8b6-4b6d-9142-38eb447adc0c");
    public static class _ProjectAbstract extends CIType
    {
        protected _ProjectAbstract(final String _uuid)
        {
            super(_uuid);
        }
        public final CIAttribute Company = new CIAttribute(this, "Company");
        public final CIAttribute Contact = new CIAttribute(this, "Contact");
        public final CIAttribute Created = new CIAttribute(this, "Created");
        public final CIAttribute Creator = new CIAttribute(this, "Creator");
        public final CIAttribute CurrencyLink = new CIAttribute(this, "CurrencyLink");
        public final CIAttribute Date = new CIAttribute(this, "Date");
        public final CIAttribute Description = new CIAttribute(this, "Description");
        public final CIAttribute DueDate = new CIAttribute(this, "DueDate");
        public final CIAttribute Lead = new CIAttribute(this, "Lead");
        public final CIAttribute Modified = new CIAttribute(this, "Modified");
        public final CIAttribute Modifier = new CIAttribute(this, "Modifier");
        public final CIAttribute Name = new CIAttribute(this, "Name");
        public final CIAttribute Note = new CIAttribute(this, "Note");
        public final CIAttribute ProjectTypeLink = new CIAttribute(this, "ProjectTypeLink");
        public final CIAttribute StatusAbstract = new CIAttribute(this, "StatusAbstract");
    }
}
