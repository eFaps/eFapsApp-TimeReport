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


package org.efaps.esjp.timereport.util;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * eFapsApp-Sales
 */
@EFapsUUID("3861c8fd-6e6a-4d0d-b662-7824a528f5ee")
@EFapsApplication("eFapsApp-TimeReport")
public interface TimereportSettings
{
    /**
     * Base string.
     */
    String BASE = "org.efaps.timereport.";

    /**
     * DateGroup for the PorsitionAnalyseReport when shown inside a project.
     */
    String PARDATEGRP4PRJT = TimereportSettings.BASE + "PositionAnalyseReportDateGroup4Project";

    /**
     * Properties. Can be concatenated.
     */
    String ABSENCECONFIG = TimereportSettings.BASE + "AbsenceConfiguration";
}
