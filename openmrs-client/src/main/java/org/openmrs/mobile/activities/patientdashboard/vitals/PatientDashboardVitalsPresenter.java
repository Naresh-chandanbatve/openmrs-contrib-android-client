/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.mobile.activities.patientdashboard.vitals;

import com.openmrs.android_sdk.library.dao.EncounterDAO;
import com.openmrs.android_sdk.library.dao.PatientDAO;
import com.openmrs.android_sdk.library.models.Patient;
import com.openmrs.android_sdk.utilities.NetworkUtils;

import org.openmrs.mobile.activities.patientdashboard.PatientDashboardContract;
import org.openmrs.mobile.activities.patientdashboard.PatientDashboardMainPresenterImpl;
import com.openmrs.android_sdk.library.api.repository.VisitRepository;
import com.openmrs.android_sdk.library.listeners.retrofitcallbacks.DefaultResponseCallback;

import rx.android.schedulers.AndroidSchedulers;

public class PatientDashboardVitalsPresenter extends PatientDashboardMainPresenterImpl implements PatientDashboardContract.PatientVitalsPresenter {
    private EncounterDAO encounterDAO;
    private VisitRepository visitRepository;
    private PatientDashboardContract.ViewPatientVitals mPatientVitalsView;

    public PatientDashboardVitalsPresenter(String id, PatientDashboardContract.ViewPatientVitals mPatientVitalsView) {
        this.mPatient = new PatientDAO().findPatientByID(id);
        this.mPatientVitalsView = mPatientVitalsView;
        this.mPatientVitalsView.setPresenter(this);
        this.encounterDAO = new EncounterDAO();
        this.visitRepository = new VisitRepository();
    }

    public PatientDashboardVitalsPresenter(Patient patient, PatientDashboardContract.ViewPatientVitals mPatientVitalsView,
                                           EncounterDAO encounterDAO, VisitRepository visitRepository) {
        this.mPatient = patient;
        this.mPatientVitalsView = mPatientVitalsView;
        this.mPatientVitalsView.setPresenter(this);
        this.encounterDAO = encounterDAO;
        this.visitRepository = visitRepository;
    }

    @Override
    public void subscribe() {
        loadVitalsFromDB();
        loadVitalsFromServer();
    }

    private void loadVitalsFromServer() {
        if (NetworkUtils.isOnline()) {
            visitRepository.syncLastVitals(mPatient.getUuid(), new DefaultResponseCallback() {
                @Override
                public void onResponse() {
                    loadVitalsFromDB();
                }

                @Override
                public void onErrorResponse(String errorMessage) {
                    mPatientVitalsView.showErrorToast(errorMessage);
                }
            });
        }
    }

    private void loadVitalsFromDB() {
        addSubscription(encounterDAO.getLastVitalsEncounter(mPatient.getUuid())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(encounter -> {
                if (encounter != null) {
                    mPatientVitalsView.showEncounterVitals(encounter);
                } else {
                    mPatientVitalsView.showNoVitalsNotification();
                }
            }));
    }

    @Override
    public void startFormDisplayActivityWithEncounter() {
        addSubscription(encounterDAO.getLastVitalsEncounter(mPatient.getUuid())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(encounter -> mPatientVitalsView.startFormDisplayActivity(encounter)));
    }
}
