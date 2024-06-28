package uk.gov.justice.digital.hmpps.adjustments.api.wiremock

const val G4946VC_PRISONER = """
  {
    "offenderNo": "G4946VC",
    "bookingId": 777831,
    "bookingNo": "K63821",
    "offenderId": 1842838,
    "rootOffenderId": 1842838,
    "firstName": "AIANILAN",
    "middleName": "JAXERIA",
    "lastName": "CHRISALD",
    "dateOfBirth": "1973-04-28",
    "age": 51,
    "activeFlag": true,
    "facialImageId": 1690777,
    "agencyId": "KMI",
    "assignedLivingUnitId": 72216,
    "religion": "Church of England (Anglican)",
    "alertsCodes": [
        "P",
        "R",
        "D",
        "X",
        "O"
    ],
    "activeAlertCount": 10,
    "inactiveAlertCount": 8,
    "alerts": [
        {
            "alertId": 1,
            "alertType": "H",
            "alertTypeDescription": "Self Harm",
            "alertCode": "HA",
            "alertCodeDescription": "ACCT Open (HMPS)",
            "dateCreated": "2011-02-03",
            "dateExpires": "2011-02-08",
            "modifiedDateTime": "2017-05-09T21:49:51.626342",
            "expired": true,
            "active": false,
            "addedByFirstName": "XTAG",
            "addedByLastName": "XTAG",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 2,
            "alertType": "R",
            "alertTypeDescription": "Risk",
            "alertCode": "ROM",
            "alertCodeDescription": "OASys Serious Harm-Medium",
            "dateCreated": "2011-01-26",
            "dateExpires": "2014-03-05",
            "modifiedDateTime": "2017-05-09T21:49:51.626428",
            "expired": true,
            "active": false,
            "addedByFirstName": "XTAG",
            "addedByLastName": "XTAG",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 3,
            "alertType": "X",
            "alertTypeDescription": "Security",
            "alertCode": "XNR",
            "alertCodeDescription": "Not For Release",
            "comment": "szmGpMPpzttqszmGpMPpzttq",
            "dateCreated": "2013-08-22",
            "modifiedDateTime": "2017-05-09T21:49:57.574318",
            "expired": false,
            "active": true,
            "addedByFirstName": "IKASTHURAI",
            "addedByLastName": "PHOEBITHA",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 4,
            "alertType": "X",
            "alertTypeDescription": "Security",
            "alertCode": "XVL",
            "alertCodeDescription": "Violent",
            "comment": "kBMozmVVMMpFysIGFDzovzJNFCJyGTCOSZtFRIvtnWtvpKkSRVbBRqRqEWnGGXkfmiLPBDzsZtklJugXYlzYKTEWvgMXffYaFFLrOkLrPMmbkxNcHmeQqoBiPXmhjXSeyvcXesOnxmzukBMozmVVMMpFysIGFDzovzJNFCJyGTCOSZtFRIvtnWtvpKkSRVbBRqRqEWnGGXkfmiLPBDzsZtklJugXYlzYKTEWvgMXffYaFFLrOkLrPMmbkxNcHmeQqoBiPXmhjXSeyvcXesOnxmzu",
            "dateCreated": "2013-11-06",
            "modifiedDateTime": "2017-05-09T21:50:15.054223",
            "expired": false,
            "active": true,
            "addedByFirstName": "DOPISKAS",
            "addedByLastName": "SOFELLE",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 5,
            "alertType": "T",
            "alertTypeDescription": "Hold Against Transfer",
            "alertCode": "TAH",
            "alertCodeDescription": "Allocation Hold",
            "comment": "tlSfjbfgBgPlLrgAItlSfjbfgBgPlLrgA",
            "dateCreated": "2013-11-26",
            "dateExpires": "2013-12-10",
            "modifiedDateTime": "2017-05-09T21:50:22.956141",
            "expired": true,
            "active": false,
            "addedByFirstName": "INFSA",
            "addedByLastName": "AVORES",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 6,
            "alertType": "T",
            "alertTypeDescription": "Hold Against Transfer",
            "alertCode": "TAP",
            "alertCodeDescription": "Accredited Programme hold",
            "comment": "CaxzErCaxzE",
            "dateCreated": "2014-01-09",
            "dateExpires": "2014-05-31",
            "modifiedDateTime": "2017-05-09T21:50:33.186852",
            "expired": true,
            "active": false,
            "addedByFirstName": "ASERTAYE",
            "addedByLastName": "MAGGANNA",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 7,
            "alertType": "P",
            "alertTypeDescription": "MAPPP Case",
            "alertCode": "P0",
            "alertCodeDescription": "MAPPA Nominal",
            "comment": "mkPKCULGRBcRNrqimkPKCULGRBcRNrqi",
            "dateCreated": "2014-02-17",
            "modifiedDateTime": "2017-05-09T21:50:41.128717",
            "expired": false,
            "active": true,
            "addedByFirstName": "DOPISKAS",
            "addedByLastName": "MARTHILLE",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 8,
            "alertType": "R",
            "alertTypeDescription": "Risk",
            "alertCode": "ROH",
            "alertCodeDescription": "OASys Serious Harm-High",
            "dateCreated": "2014-03-05",
            "modifiedDateTime": "2017-05-09T21:50:52.14323",
            "expired": false,
            "active": true,
            "addedByFirstName": "IKASTHURAI",
            "addedByLastName": "KENEH",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 9,
            "alertType": "O",
            "alertTypeDescription": "Other",
            "alertCode": "OIOM",
            "alertCodeDescription": "Integrated Offender Management Case",
            "comment": "xuQywBZznxaiCjJybxuQywBZznxaiCjJyb",
            "dateCreated": "2014-03-05",
            "modifiedDateTime": "2017-05-09T21:50:52.143552",
            "expired": false,
            "active": true,
            "addedByFirstName": "IKASTHURAI",
            "addedByLastName": "KENEH",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 10,
            "alertType": "H",
            "alertTypeDescription": "Self Harm",
            "alertCode": "HA",
            "alertCodeDescription": "ACCT Open (HMPS)",
            "comment": "nfZOgqGgNRPuIwSQnmohZaFgYrXtARkpUDSNwDWcOMxAZYvwWhEtDCXMXwHgIqWhUhHFAptBzliyzAdvYcmEUHBsAZuEusKjuFFaOoyVTmsDDBHwZtldyXZVHBKuhDltrnYNpcRsPeHYIJoZTfisQjEBGPVwsjPKaRQtdZooXtBHhOuahsnynpunfZOgqGgNRPuIwSQnmohZaFgYrXtARkpUDSNwDWcOMxAZYvwWhEtDCXMXwHgIqWhUhHFAptBzliyzAdvYcmEUHBsAZuEusKjuFFaOoyVTmsDDBHwZtldyXZVHBKuhDltrnYNpcRsPeHYIJoZTfisQjEBGPVwsjPKaRQtdZooXtBHhOuahsnynpu",
            "dateCreated": "2015-02-20",
            "dateExpires": "2015-02-25",
            "modifiedDateTime": "2017-05-09T21:52:33.548762",
            "expired": true,
            "active": false,
            "addedByFirstName": "YBRDYRAKUMAR",
            "addedByLastName": "LEONEPH",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 11,
            "alertType": "R",
            "alertTypeDescription": "Risk",
            "alertCode": "RKS",
            "alertCodeDescription": "Risk to Known Adult - Custody",
            "comment": "EHcwbNOZgfEKGRcsiFEYEDpKSDiuoDyEEHcwbNOZgfEKGRcsiFEYEDpKSDiuoDy",
            "dateCreated": "2015-03-12",
            "modifiedDateTime": "2017-05-09T21:52:46.518092",
            "expired": false,
            "active": true,
            "addedByFirstName": "APMONDU",
            "addedByLastName": "GEROTHY",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 12,
            "alertType": "H",
            "alertTypeDescription": "Self Harm",
            "alertCode": "HA",
            "alertCodeDescription": "ACCT Open (HMPS)",
            "comment": "MsJVLRrglbcTYoWdknguacUiRAJlcrnwHCgghNiOCFiYTyibzcyXUmKesSGnIfcseIzFBFrmtOStaCKRcIIwdyaAVmTpsLGgOheUDaOtqRQvpUvVbOkmdDtzuTvtQyTHwwOecYKTaZrSTVVwBDcVOAgJAZOUYhBnOfZSsFoBocFwYOwvXXPeyzQxWdyBFLGpFRlMbJxpghSnOcEXwezXhRklIYTyRvqttqIAYulDEiSNcVVSIyZndraNpEKsFoiZXbnnWqbBNRlJnGDQWtlumQHjvTtdiUdcKFByLwUVPKMsJVLRrglbcTYoWdknguacUiRAJlcrnwHCgghNiOCFiYTyibzcyXUmKesSGnIfcseIzFBFrmtOStaCKRcIIwdyaAVmTpsLGgOheUDaOtqRQvpUvVbOkmdDtzuTvtQyTHwwOecYKTaZrSTVVwBDcVOAgJAZOUYhBnOfZSsFoBocFwYOwvXXPeyzQxWdyBFLGpFRlMbJxpghSnOcEXwezXhRklIYTyRvqttqIAYulDEiSNcVVSIyZndraNpEKsFoiZXbnnWqbBNRlJnGDQWtlumQHjvTtdiUdcKFByLwUVP",
            "dateCreated": "2015-03-09",
            "dateExpires": "2015-03-17",
            "modifiedDateTime": "2017-05-09T21:52:44.702204",
            "expired": true,
            "active": false,
            "addedByFirstName": "YBRDYRAKUMAR",
            "addedByLastName": "LEONEPH",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 13,
            "alertType": "H",
            "alertTypeDescription": "Self Harm",
            "alertCode": "HA",
            "alertCodeDescription": "ACCT Open (HMPS)",
            "comment": "ExjSyUwZhTCcrDfGsqOBzRnVygExjSyUwZhTCcrDfGsqOBzRnVy",
            "dateCreated": "2015-04-02",
            "dateExpires": "2015-05-15",
            "modifiedDateTime": "2017-05-09T21:52:52.591505",
            "expired": true,
            "active": false,
            "addedByFirstName": "APMONDU",
            "addedByLastName": "JAYMENTINO",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 14,
            "alertType": "R",
            "alertTypeDescription": "Risk",
            "alertCode": "RPB",
            "alertCodeDescription": "Risk to Public - Community",
            "comment": "lLwuCmYvbvygyPUwrkVqkTDEyohgssmkYGnDlLwuCmYvbvygyPUwrkVqkTDEyohgssmkYGnD",
            "dateCreated": "2015-06-26",
            "modifiedDateTime": "2017-05-09T21:53:24.515239",
            "expired": false,
            "active": true,
            "addedByFirstName": "ASRULOR",
            "addedByLastName": "SHIN",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 15,
            "alertType": "R",
            "alertTypeDescription": "Risk",
            "alertCode": "RKC",
            "alertCodeDescription": "Risk to Known Adult - Community",
            "comment": "FdDStlbjBDHBQIQAoWNkAeaveLgzCmkDtXPlWwalXLGPxyFdDStlbjBDHBQIQAoWNkAeaveLgzCmkDtXPlWwalXLGPx",
            "dateCreated": "2015-06-26",
            "modifiedDateTime": "2017-05-09T21:53:24.5155",
            "expired": false,
            "active": true,
            "addedByFirstName": "ASRULOR",
            "addedByLastName": "SHIN",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 16,
            "alertType": "R",
            "alertTypeDescription": "Risk",
            "alertCode": "RDV",
            "alertCodeDescription": "Domestic Violence Perpetrator",
            "comment": "ghOAVYkNyjojRwYLmubWmnFqlMhTAxcHctLBXnAjmhHghuINfzitCCoSuwUVVNUbdAlokYvZciDUiNDRLPNCzQZXqgXtAVLQitVSTDmIilNlPskFMhYVeuyTgJiktGqKghOAVYkNyjojRwYLmubWmnFqlMhTAxcHctLBXnAjmhHghuINfzitCCoSuwUVVNUbdAlokYvZciDUiNDRLPNCzQZXqgXtAVLQitVSTDmIilNlPskFMhYVeuyTgJiktGqK",
            "dateCreated": "2015-06-26",
            "modifiedDateTime": "2017-05-09T21:53:24.515966",
            "expired": false,
            "active": true,
            "addedByFirstName": "ASRULOR",
            "addedByLastName": "SHIN",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 17,
            "alertType": "R",
            "alertTypeDescription": "Risk",
            "alertCode": "RCC",
            "alertCodeDescription": "Risk to Children - Community",
            "comment": "cYmBRqZuBbnPJJcCNvOePKkOCDCNJdSKKAnhjJzOIMlnEbWtcYmBRqZuBbnPJJcCNvOePKkOCDCNJdSKKAnhjJzOIMlnEbW",
            "dateCreated": "2015-06-26",
            "dateExpires": "2015-08-14",
            "modifiedDateTime": "2017-05-09T21:53:24.516244",
            "expired": true,
            "active": false,
            "addedByFirstName": "ASRULOR",
            "addedByLastName": "SHIN",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        },
        {
            "alertId": 18,
            "alertType": "D",
            "alertTypeDescription": "Security. Do not share with offender",
            "alertCode": "DOCGM",
            "alertCodeDescription": "OCG Nominal - Do not share",
            "comment": "** Offenders must not be made aware of the OCG flag status. Do not Share with offender. **\n\nEPPsMxMBJiXuMxYoPmGPVqRJTLcicYAvjGVgRfXtUpvTAjHhbmNyYxVYbrapVNwsotbecwikbCjWaYqvxpgAWWTKywVnsUmJiXydgpRCoxUyWLTZhGqsjPDnqxSVRUtKZizGNHxmmAzshmjgDMfbkpxZUREPPsMxMBJiXuMxYoPmGPVqRJTLcicYAvjGVgRfXtUpvTAjHhbmNyYxVYbrapVNwsotbecwikbCjWaYqvxpgAWWTKywVnsUmJiXydgpRCoxUyWLTZhGqsjPDnqxSVRUtKZizGNHxmmAzshmjgDMfbkpxZU",
            "dateCreated": "2015-08-04",
            "modifiedDateTime": "2020-08-06T12:03:56.760711",
            "expired": false,
            "active": true,
            "addedByFirstName": "OLNHEWJAN",
            "addedByLastName": "CHRISALD",
            "expiredByFirstName": "ADMIN&ONB",
            "expiredByLastName": "CNOMIS"
        }
    ],
    "assignedLivingUnit": {
        "agencyId": "KMI",
        "locationId": 72216,
        "description": "E-7-025",
        "agencyName": "Kirkham (HMP)"
    },
    "physicalAttributes": {
        "sexCode": "M",
        "gender": "Male",
        "raceCode": "W1",
        "ethnicity": "White: Eng./Welsh/Scot./N.Irish/British",
        "heightFeet": 5,
        "heightInches": 9,
        "heightMetres": 1.75,
        "heightCentimetres": 175,
        "weightPounds": 154,
        "weightKilograms": 70
    },
    "physicalCharacteristics": [
        {
            "type": "HAIR",
            "characteristic": "Hair Colour",
            "detail": "Brown"
        },
        {
            "type": "R_EYE_C",
            "characteristic": "Right Eye Colour",
            "detail": "Blue"
        },
        {
            "type": "L_EYE_C",
            "characteristic": "Left Eye Colour",
            "detail": "Blue"
        },
        {
            "type": "FACIAL_HAIR",
            "characteristic": "Facial Hair",
            "detail": "Clean Shaven"
        },
        {
            "type": "FACE",
            "characteristic": "Shape of Face",
            "detail": "Oval"
        },
        {
            "type": "BUILD",
            "characteristic": "Build",
            "detail": "Medium"
        }
    ],
    "profileInformation": [
        {
            "type": "YOUTH",
            "question": "Youth Offender?",
            "resultValue": "No"
        },
        {
            "type": "MARITAL",
            "question": "Domestic Status",
            "resultValue": "Single-not married/in civil partnership"
        },
        {
            "type": "CHILD",
            "question": "Number of Children?",
            "resultValue": "5"
        },
        {
            "type": "SMOKE",
            "question": "Is the Offender a smoker?",
            "resultValue": "Yes"
        },
        {
            "type": "IMM",
            "question": "Interest to Immigration?",
            "resultValue": "No"
        },
        {
            "type": "SEXO",
            "question": "Sexual Orientation",
            "resultValue": "Heterosexual / Straight"
        },
        {
            "type": "RELF",
            "question": "Religion",
            "resultValue": "Church of England (Anglican)"
        },
        {
            "type": "NAT",
            "question": "Nationality?",
            "resultValue": "British"
        }
    ],
    "physicalMarks": [
        {
            "type": "Other",
            "bodyPart": "Torso",
            "comment": "nxJUznnwQXttafSqmkdVLwVtUoGCzlnxJUznnwQXttafSqmkdVLwVtUoGCz"
        }
    ],
    "assessments": [
        {
            "bookingId": 777831,
            "classificationCode": "STANDARD",
            "classification": "Standard",
            "assessmentCode": "CSR",
            "assessmentDescription": "CSR Rating",
            "cellSharingAlertFlag": true,
            "assessmentDate": "2015-06-24",
            "nextReviewDate": "2015-06-25",
            "assessmentStatus": "A",
            "assessmentSeq": 7
        },
        {
            "bookingId": 777831,
            "classificationCode": "D",
            "classification": "Cat D",
            "assessmentCode": "CATEGORY",
            "assessmentDescription": "Categorisation",
            "cellSharingAlertFlag": false,
            "assessmentDate": "2016-09-19",
            "nextReviewDate": "2018-08-11",
            "assessmentStatus": "A",
            "assessmentSeq": 11
        }
    ],
    "csra": "Standard",
    "category": "Cat D",
    "categoryCode": "D",
    "birthPlace": "LIVERPOOL",
    "birthCountryCode": "ENG",
    "inOutStatus": "IN",
    "identifiers": [
        {
            "type": "CRO",
            "value": "181371/88Q",
            "offenderNo": "G4946VC",
            "caseloadType": "INST",
            "whenCreated": "2012-03-04T05:28:41.472179",
            "offenderId": 1842838,
            "rootOffenderId": 1842838
        },
        {
            "type": "PNC",
            "value": "88/181371F",
            "offenderNo": "G4946VC",
            "caseloadType": "INST",
            "whenCreated": "2012-03-04T05:28:41.483153",
            "offenderId": 1842838,
            "rootOffenderId": 1842838
        },
        {
            "type": "ULN",
            "value": "1408710144",
            "offenderNo": "G4946VC",
            "issuedDate": "2015-07-01",
            "caseloadType": "INST",
            "whenCreated": "2015-07-01T11:24:54.818666",
            "offenderId": 1842838,
            "rootOffenderId": 1842838
        }
    ],
    "personalCareNeeds": [
        {
            "personalCareNeedId": 655415,
            "problemType": "DISAB",
            "problemCode": "ND",
            "problemStatus": "ON",
            "problemDescription": "No Disability",
            "commentText": null,
            "startDate": "2014-02-03",
            "endDate": null
        }
    ],
    "sentenceDetail": {
        "sentenceExpiryDate": "2023-03-22",
        "conditionalReleaseDate": "2018-09-18",
        "licenceExpiryDate": "2023-03-14",
        "effectiveSentenceEndDate": "2023-03-10",
        "bookingId": 777831,
        "sentenceStartDate": "2010-10-31",
        "additionalDaysAwarded": 8,
        "nonDtoReleaseDate": "2018-09-18",
        "sentenceExpiryCalculatedDate": "2023-03-22",
        "licenceExpiryCalculatedDate": "2023-03-14",
        "nonDtoReleaseDateType": "CRD",
        "confirmedReleaseDate": "2018-08-11",
        "releaseDate": "2018-08-11"
    },
    "offenceHistory": [
        {
            "bookingId": 777831,
            "offenceDate": "2010-04-05",
            "offenceDescription": "Possess with intent to supply a controlled drug of Class A - Cocaine",
            "offenceCode": "MD71230",
            "statuteCode": "MD71",
            "mostSerious": false,
            "primaryResultCode": "1501",
            "primaryResultDescription": "Recall to Prison",
            "primaryResultConviction": true,
            "secondaryResultConviction": false,
            "courtDate": "2010-09-15",
            "caseId": 964792
        },
        {
            "bookingId": 777831,
            "offenceDate": "2010-04-14",
            "offenceDescription": "Possess with intent to supply a controlled drug of Class A - Heroin",
            "offenceCode": "MD71231",
            "statuteCode": "MD71",
            "mostSerious": false,
            "primaryResultCode": "1501",
            "primaryResultDescription": "Recall to Prison",
            "primaryResultConviction": true,
            "secondaryResultConviction": false,
            "courtDate": "2010-09-15",
            "caseId": 964792
        },
        {
            "bookingId": 777831,
            "offenceDescription": "Robbery",
            "offenceCode": "TH68023",
            "statuteCode": "TH68",
            "mostSerious": true,
            "primaryResultCode": "1002",
            "primaryResultDescription": "Imprisonment",
            "primaryResultConviction": true,
            "secondaryResultConviction": false,
            "courtDate": "2014-02-14",
            "caseId": 957837
        },
        {
            "bookingId": 777831,
            "offenceDescription": "Aggravated vehicle taking - ( driver did not take ) and dangerous driving",
            "offenceCode": "TH68147",
            "statuteCode": "TH68",
            "mostSerious": false,
            "primaryResultCode": "1002",
            "primaryResultDescription": "Imprisonment",
            "primaryResultConviction": true,
            "secondaryResultConviction": false,
            "courtDate": "2014-02-14",
            "caseId": 957837
        }
    ],
    "sentenceTerms": [
        {
            "bookingId": 777831,
            "sentenceSequence": 1,
            "termSequence": 1,
            "sentenceType": "LR",
            "sentenceTypeDescription": "Licence Recall",
            "startDate": "2010-10-31",
            "years": 5,
            "months": 6,
            "lifeSentence": false,
            "caseId": "964792",
            "sentenceTermCode": "IMP",
            "lineSeq": 1,
            "sentenceStartDate": "2010-11-06"
        },
        {
            "bookingId": 777831,
            "sentenceSequence": 2,
            "termSequence": 1,
            "sentenceType": "ADIMP",
            "sentenceTypeDescription": "CJA03 Standard Determinate Sentence",
            "startDate": "2014-03-29",
            "years": 8,
            "months": 6,
            "lifeSentence": false,
            "caseId": "957837",
            "sentenceTermCode": "IMP",
            "lineSeq": 2,
            "sentenceStartDate": "2014-03-14"
        },
        {
            "bookingId": 777831,
            "sentenceSequence": 3,
            "termSequence": 1,
            "consecutiveTo": 2,
            "sentenceType": "ADIMP",
            "sentenceTypeDescription": "CJA03 Standard Determinate Sentence",
            "startDate": "2022-09-21",
            "months": 6,
            "lifeSentence": false,
            "caseId": "957837",
            "sentenceTermCode": "IMP",
            "lineSeq": 3,
            "sentenceStartDate": "2022-09-18"
        }
    ],
    "aliases": [
        {
            "firstName": "OGONANTHOMASIN",
            "lastName": "TONERTO",
            "age": 50,
            "dob": "1974-05-22",
            "gender": "Male",
            "createDate": "2015-06-29",
            "offenderId": 2361712
        },
        {
            "firstName": "OGONANTHOMASIN",
            "lastName": "CIERRUMN",
            "age": 51,
            "dob": "1973-05-09",
            "gender": "Male",
            "createDate": "2015-06-29",
            "offenderId": 2361713
        },
        {
            "firstName": "OGONANTHOMASIN",
            "lastName": "JARVINE",
            "age": 51,
            "dob": "1973-05-05",
            "gender": "Male",
            "createDate": "2015-06-29",
            "offenderId": 2361714
        },
        {
            "firstName": "OGONANTHOMASIN",
            "middleName": "RYLOR",
            "lastName": "MORESTE",
            "age": 51,
            "dob": "1973-05-28",
            "gender": "Male",
            "createDate": "2015-06-29",
            "offenderId": 2361715
        },
        {
            "firstName": "OGONANTHOMASIN",
            "lastName": "NINICE",
            "age": 51,
            "dob": "1973-05-14",
            "gender": "Male",
            "createDate": "2015-06-29",
            "offenderId": 2361723
        },
        {
            "firstName": "OGONANTHOMASIN",
            "lastName": "JANONTA",
            "age": 51,
            "dob": "1973-05-11",
            "gender": "Male",
            "createDate": "2015-06-29",
            "offenderId": 2361720
        },
        {
            "firstName": "OGONANTHOMASIN",
            "lastName": "CLIFFID",
            "age": 51,
            "dob": "1973-05-26",
            "gender": "Male",
            "createDate": "2015-06-29",
            "offenderId": 2361721
        },
        {
            "firstName": "OGONANTHOMASIN",
            "lastName": "AILICK",
            "age": 51,
            "dob": "1973-05-18",
            "gender": "Male",
            "createDate": "2015-06-29",
            "offenderId": 2361722
        },
        {
            "firstName": "OGONANTHOMASIN",
            "middleName": "JAXONTA",
            "lastName": "VANITH",
            "age": 51,
            "dob": "1973-05-08",
            "gender": "Male",
            "createDate": "2015-06-29",
            "offenderId": 2361719
        }
    ],
    "status": "ACTIVE IN",
    "statusReason": "ADM-INT",
    "lastMovementTypeCode": "ADM",
    "lastMovementReasonCode": "INT",
    "legalStatus": "SENTENCED",
    "recall": true,
    "imprisonmentStatus": "SENT03",
    "imprisonmentStatusDescription": "Adult Imprisonment Without Option CJA03",
    "receptionDate": "2013-08-10",
    "locationDescription": "Kirkham (HMP)",
    "latestLocationId": "KMI"
}
"""

const val G4946VC_SENTENCES_AND_OFFENCES = """
 [
   {
     "bookingId": 777831,
     "sentenceSequence": 1,
     "lineSequence": 1,
     "caseSequence": 2,
     "caseReference": "L/R -22/08/13",
     "courtDescription": "Liverpool Crown Court",
     "sentenceStatus": "A",
     "sentenceCategory": "2003",
     "sentenceCalculationType": "LR",
     "sentenceTypeDescription": "Licence Recall",
     "sentenceDate": "2010-09-15",
     "sentenceStartDate": "2010-11-06",
     "sentenceEndDate": "2016-05-08",
     "terms": [
       {
         "years": 5,
         "months": 6,
         "weeks": 0,
         "days": 0,
         "code": "IMP"
       }
     ],
     "offences": [
       {
         "offenderChargeId": 2247352,
         "offenceStartDate": "2010-04-14",
         "offenceStatute": "MD71",
         "offenceCode": "MD71231",
         "offenceDescription": "Possess with intent to supply a controlled drug of Class A - Heroin",
         "indicators": [
           "D",
           "87",
           "S"
         ]
       },
       {
         "offenderChargeId": 2247353,
         "offenceStartDate": "2010-04-05",
         "offenceStatute": "MD71",
         "offenceCode": "MD71230",
         "offenceDescription": "Possess with intent to supply a controlled drug of Class A - Cocaine",
         "indicators": [
           "D",
           "87"
         ]
       }
     ]
   },
   {
     "bookingId": 777831,
     "sentenceSequence": 2,
     "lineSequence": 2,
     "caseSequence": 1,
     "caseReference": "T20137110",
     "courtDescription": "Shrewsbury Crown Court",
     "sentenceStatus": "A",
     "sentenceCategory": "2003",
     "sentenceCalculationType": "ADIMP",
     "sentenceTypeDescription": "CJA03 Standard Determinate Sentence",
     "sentenceDate": "2014-02-14",
     "sentenceStartDate": "2014-03-14",
     "sentenceEndDate": "2022-08-29",
     "terms": [
       {
         "years": 8,
         "months": 6,
         "weeks": 0,
         "days": 0,
         "code": "IMP"
       }
     ],
     "offences": [
       {
         "offenderChargeId": 2228470,
         "offenceStatute": "TH68",
         "offenceCode": "TH68023",
         "offenceDescription": "Robbery",
         "indicators": [
           "ERS",
           "D",
           "V",
           "48",
           "SCH17A2",
           "PCSC/SDS+",
           "S15/CJIB",
           "SCH15/CJIB/L"
         ]
       }
     ]
   },
   {
     "bookingId": 777831,
     "sentenceSequence": 3,
     "consecutiveToSequence": 2,
     "lineSequence": 3,
     "caseSequence": 1,
     "caseReference": "T20137110",
     "courtDescription": "Shrewsbury Crown Court",
     "sentenceStatus": "A",
     "sentenceCategory": "2003",
     "sentenceCalculationType": "ADIMP",
     "sentenceTypeDescription": "CJA03 Standard Determinate Sentence",
     "sentenceDate": "2014-02-14",
     "sentenceStartDate": "2022-09-18",
     "sentenceEndDate": "2023-03-18",
     "terms": [
       {
         "years": 0,
         "months": 6,
         "weeks": 0,
         "days": 0,
         "code": "IMP"
       }
     ],
     "offences": [
       {
         "offenderChargeId": 2228471,
         "offenceStatute": "TH68",
         "offenceCode": "TH68147",
         "offenceDescription": "Aggravated vehicle taking - ( driver did not take ) and dangerous driving",
         "indicators": [
           "D",
           "18"
         ]
       }
     ]
   }
 ]
"""
const val G4946VC_COURT_DATE_RESULTS = """
  [
    {
        "id": 107376770,
        "date": "2010-03-01",
        "resultCode": "1002",
        "resultDescription": "Imprisonment",
        "resultDispositionCode": "F",
        "charge": {
            "chargeId": 1525217,
            "offenceCode": "MD71230-245N",
            "offenceStatue": "ZZ",
            "offenceDescription": "POSSESSION DRUGS WITH INTENT TO SUPPLY",
            "offenceDate": "2010-03-13",
            "guilty": false,
            "courtCaseId": 689225,
            "courtLocation": "Liverpool Crown Court",
            "sentenceSequence": 1,
            "sentenceDate": "2010-09-15",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 594299,
        "bookNumber": "GX8150"
    },
    {
        "id": 107376770,
        "date": "2010-03-01",
        "resultCode": "1002",
        "resultDescription": "Imprisonment",
        "resultDispositionCode": "F",
        "charge": {
            "chargeId": 1525218,
            "offenceCode": "MD71230-245N",
            "offenceStatue": "ZZ",
            "offenceDescription": "POSSESSION DRUGS WITH INTENT TO SUPPLY",
            "offenceDate": "2010-04-05",
            "guilty": false,
            "courtCaseId": 689225,
            "courtLocation": "Liverpool Crown Court",
            "sentenceSequence": 2,
            "sentenceDate": "2010-09-15",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 594299,
        "bookNumber": "GX8150"
    },
    {
        "id": 107376772,
        "date": "2010-04-06",
        "resultCode": "1002",
        "resultDescription": "Imprisonment",
        "resultDispositionCode": "F",
        "charge": {
            "chargeId": 1525217,
            "offenceCode": "MD71230-245N",
            "offenceStatue": "ZZ",
            "offenceDescription": "POSSESSION DRUGS WITH INTENT TO SUPPLY",
            "offenceDate": "2010-03-13",
            "guilty": false,
            "courtCaseId": 689225,
            "courtLocation": "Liverpool Crown Court",
            "sentenceSequence": 1,
            "sentenceDate": "2010-09-15",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 594299,
        "bookNumber": "GX8150"
    },
    {
        "id": 107376772,
        "date": "2010-04-06",
        "resultCode": "1002",
        "resultDescription": "Imprisonment",
        "resultDispositionCode": "F",
        "charge": {
            "chargeId": 1525218,
            "offenceCode": "MD71230-245N",
            "offenceStatue": "ZZ",
            "offenceDescription": "POSSESSION DRUGS WITH INTENT TO SUPPLY",
            "offenceDate": "2010-04-05",
            "guilty": false,
            "courtCaseId": 689225,
            "courtLocation": "Liverpool Crown Court",
            "sentenceSequence": 2,
            "sentenceDate": "2010-09-15",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 594299,
        "bookNumber": "GX8150"
    },
    {
        "id": 107376769,
        "date": "2010-08-18",
        "resultCode": "1002",
        "resultDescription": "Imprisonment",
        "resultDispositionCode": "F",
        "charge": {
            "chargeId": 1525217,
            "offenceCode": "MD71230-245N",
            "offenceStatue": "ZZ",
            "offenceDescription": "POSSESSION DRUGS WITH INTENT TO SUPPLY",
            "offenceDate": "2010-03-13",
            "guilty": false,
            "courtCaseId": 689225,
            "courtLocation": "Liverpool Crown Court",
            "sentenceSequence": 1,
            "sentenceDate": "2010-09-15",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 594299,
        "bookNumber": "GX8150"
    },
    {
        "id": 107376769,
        "date": "2010-08-18",
        "resultCode": "1002",
        "resultDescription": "Imprisonment",
        "resultDispositionCode": "F",
        "charge": {
            "chargeId": 1525218,
            "offenceCode": "MD71230-245N",
            "offenceStatue": "ZZ",
            "offenceDescription": "POSSESSION DRUGS WITH INTENT TO SUPPLY",
            "offenceDate": "2010-04-05",
            "guilty": false,
            "courtCaseId": 689225,
            "courtLocation": "Liverpool Crown Court",
            "sentenceSequence": 2,
            "sentenceDate": "2010-09-15",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 594299,
        "bookNumber": "GX8150"
    },
    {
        "id": 107376771,
        "date": "2010-09-15",
        "resultCode": "1002",
        "resultDescription": "Imprisonment",
        "resultDispositionCode": "F",
        "charge": {
            "chargeId": 1525217,
            "offenceCode": "MD71230-245N",
            "offenceStatue": "ZZ",
            "offenceDescription": "POSSESSION DRUGS WITH INTENT TO SUPPLY",
            "offenceDate": "2010-03-13",
            "guilty": false,
            "courtCaseId": 689225,
            "courtLocation": "Liverpool Crown Court",
            "sentenceSequence": 1,
            "sentenceDate": "2010-09-15",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 594299,
        "bookNumber": "GX8150"
    },
    {
        "id": 107376771,
        "date": "2010-09-15",
        "resultCode": "1002",
        "resultDescription": "Imprisonment",
        "resultDispositionCode": "F",
        "charge": {
            "chargeId": 1525218,
            "offenceCode": "MD71230-245N",
            "offenceStatue": "ZZ",
            "offenceDescription": "POSSESSION DRUGS WITH INTENT TO SUPPLY",
            "offenceDate": "2010-04-05",
            "guilty": false,
            "courtCaseId": 689225,
            "courtLocation": "Liverpool Crown Court",
            "sentenceSequence": 2,
            "sentenceDate": "2010-09-15",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 594299,
        "bookNumber": "GX8150"
    },
    {
        "id": 177539043,
        "date": "2010-09-15",
        "resultCode": "1501",
        "resultDescription": "Recall to Prison",
        "resultDispositionCode": "F",
        "charge": {
            "chargeId": 2247352,
            "offenceCode": "MD71231",
            "offenceStatue": "MD71",
            "offenceDescription": "Possess with intent to supply a controlled drug of Class A - Heroin",
            "offenceDate": "2010-04-14",
            "guilty": false,
            "courtCaseId": 964792,
            "courtCaseRef": "L/R -22/08/13",
            "courtLocation": "Liverpool Crown Court",
            "sentenceSequence": 1,
            "sentenceDate": "2010-09-15",
            "resultDescription": "Recall to Prison"
        },
        "bookingId": 777831,
        "bookNumber": "K63821"
    },
    {
        "id": 177539043,
        "date": "2010-09-15",
        "resultCode": "1501",
        "resultDescription": "Recall to Prison",
        "resultDispositionCode": "F",
        "charge": {
            "chargeId": 2247353,
            "offenceCode": "MD71230",
            "offenceStatue": "MD71",
            "offenceDescription": "Possess with intent to supply a controlled drug of Class A - Cocaine",
            "offenceDate": "2010-04-05",
            "guilty": false,
            "courtCaseId": 964792,
            "courtCaseRef": "L/R -22/08/13",
            "courtLocation": "Liverpool Crown Court",
            "sentenceSequence": 1,
            "sentenceDate": "2010-09-15",
            "resultDescription": "Recall to Prison"
        },
        "bookingId": 777831,
        "bookNumber": "K63821"
    },
    {
        "id": 175604667,
        "date": "2013-08-10",
        "resultCode": "4565",
        "resultDescription": "Commit to Crown Court for Trial (Summary / Either Way Offences)",
        "resultDispositionCode": "I",
        "charge": {
            "chargeId": 2228470,
            "offenceCode": "TH68023",
            "offenceStatue": "TH68",
            "offenceDescription": "Robbery",
            "guilty": false,
            "courtCaseId": 957837,
            "courtCaseRef": "T20137110",
            "courtLocation": "Shrewsbury Crown Court",
            "sentenceSequence": 2,
            "sentenceDate": "2014-02-14",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 777831,
        "bookNumber": "K63821"
    },
    {
        "id": 175604667,
        "date": "2013-08-10",
        "resultCode": "4565",
        "resultDescription": "Commit to Crown Court for Trial (Summary / Either Way Offences)",
        "resultDispositionCode": "I",
        "charge": {
            "chargeId": 2228471,
            "offenceCode": "TH68147",
            "offenceStatue": "TH68",
            "offenceDescription": "Aggravated vehicle taking - ( driver did not take ) and dangerous driving",
            "guilty": false,
            "courtCaseId": 957837,
            "courtCaseRef": "T20137110",
            "courtLocation": "Shrewsbury Crown Court",
            "sentenceSequence": 3,
            "sentenceDate": "2014-02-14",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 777831,
        "bookNumber": "K63821"
    },
    {
        "id": 176669175,
        "date": "2013-08-19",
        "resultCode": "4560",
        "resultDescription": "Commit/Transfer/Send to Crown Court for Trial in Custody",
        "resultDispositionCode": "I",
        "charge": {
            "chargeId": 2228470,
            "offenceCode": "TH68023",
            "offenceStatue": "TH68",
            "offenceDescription": "Robbery",
            "guilty": false,
            "courtCaseId": 957837,
            "courtCaseRef": "T20137110",
            "courtLocation": "Shrewsbury Crown Court",
            "sentenceSequence": 2,
            "sentenceDate": "2014-02-14",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 777831,
        "bookNumber": "K63821"
    },
    {
        "id": 176669175,
        "date": "2013-08-19",
        "resultCode": "4560",
        "resultDescription": "Commit/Transfer/Send to Crown Court for Trial in Custody",
        "resultDispositionCode": "I",
        "charge": {
            "chargeId": 2228471,
            "offenceCode": "TH68147",
            "offenceStatue": "TH68",
            "offenceDescription": "Aggravated vehicle taking - ( driver did not take ) and dangerous driving",
            "guilty": false,
            "courtCaseId": 957837,
            "courtCaseRef": "T20137110",
            "courtLocation": "Shrewsbury Crown Court",
            "sentenceSequence": 3,
            "sentenceDate": "2014-02-14",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 777831,
        "bookNumber": "K63821"
    },
    {
        "id": 186814131,
        "date": "2013-11-04",
        "resultCode": "4560",
        "resultDescription": "Commit/Transfer/Send to Crown Court for Trial in Custody",
        "resultDispositionCode": "I",
        "charge": {
            "chargeId": 2228470,
            "offenceCode": "TH68023",
            "offenceStatue": "TH68",
            "offenceDescription": "Robbery",
            "guilty": false,
            "courtCaseId": 957837,
            "courtCaseRef": "T20137110",
            "courtLocation": "Shrewsbury Crown Court",
            "sentenceSequence": 2,
            "sentenceDate": "2014-02-14",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 777831,
        "bookNumber": "K63821"
    },
    {
        "id": 186814131,
        "date": "2013-11-04",
        "resultCode": "4560",
        "resultDescription": "Commit/Transfer/Send to Crown Court for Trial in Custody",
        "resultDispositionCode": "I",
        "charge": {
            "chargeId": 2228471,
            "offenceCode": "TH68147",
            "offenceStatue": "TH68",
            "offenceDescription": "Aggravated vehicle taking - ( driver did not take ) and dangerous driving",
            "guilty": false,
            "courtCaseId": 957837,
            "courtCaseRef": "T20137110",
            "courtLocation": "Shrewsbury Crown Court",
            "sentenceSequence": 3,
            "sentenceDate": "2014-02-14",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 777831,
        "bookNumber": "K63821"
    },
    {
        "id": 196131170,
        "date": "2014-01-20",
        "resultCode": "4004",
        "resultDescription": "Sentence Postponed",
        "resultDispositionCode": "I",
        "charge": {
            "chargeId": 2228470,
            "offenceCode": "TH68023",
            "offenceStatue": "TH68",
            "offenceDescription": "Robbery",
            "guilty": false,
            "courtCaseId": 957837,
            "courtCaseRef": "T20137110",
            "courtLocation": "Shrewsbury Crown Court",
            "sentenceSequence": 2,
            "sentenceDate": "2014-02-14",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 777831,
        "bookNumber": "K63821"
    },
    {
        "id": 196131170,
        "date": "2014-01-20",
        "resultCode": "4004",
        "resultDescription": "Sentence Postponed",
        "resultDispositionCode": "I",
        "charge": {
            "chargeId": 2228471,
            "offenceCode": "TH68147",
            "offenceStatue": "TH68",
            "offenceDescription": "Aggravated vehicle taking - ( driver did not take ) and dangerous driving",
            "guilty": false,
            "courtCaseId": 957837,
            "courtCaseRef": "T20137110",
            "courtLocation": "Shrewsbury Crown Court",
            "sentenceSequence": 3,
            "sentenceDate": "2014-02-14",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 777831,
        "bookNumber": "K63821"
    },
    {
        "id": 199887241,
        "date": "2014-02-14",
        "resultCode": "1002",
        "resultDescription": "Imprisonment",
        "resultDispositionCode": "F",
        "charge": {
            "chargeId": 2228470,
            "offenceCode": "TH68023",
            "offenceStatue": "TH68",
            "offenceDescription": "Robbery",
            "guilty": false,
            "courtCaseId": 957837,
            "courtCaseRef": "T20137110",
            "courtLocation": "Shrewsbury Crown Court",
            "sentenceSequence": 2,
            "sentenceDate": "2014-02-14",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 777831,
        "bookNumber": "K63821"
    },
    {
        "id": 199887241,
        "date": "2014-02-14",
        "resultCode": "1002",
        "resultDescription": "Imprisonment",
        "resultDispositionCode": "F",
        "charge": {
            "chargeId": 2228471,
            "offenceCode": "TH68147",
            "offenceStatue": "TH68",
            "offenceDescription": "Aggravated vehicle taking - ( driver did not take ) and dangerous driving",
            "guilty": false,
            "courtCaseId": 957837,
            "courtCaseRef": "T20137110",
            "courtLocation": "Shrewsbury Crown Court",
            "sentenceSequence": 3,
            "sentenceDate": "2014-02-14",
            "resultDescription": "Imprisonment"
        },
        "bookingId": 777831,
        "bookNumber": "K63821"
    }
]
"""

const val G4946VC_ADJUDICATIONS = """
  {
    "totalPages": 1,
    "totalElements": 2,
    "pageable": {
        "pageNumber": 0,
        "pageSize": 1000,
        "sort": {
            "unsorted": false,
            "sorted": true,
            "empty": false
        },
        "offset": 0,
        "unpaged": false,
        "paged": true
    },
    "numberOfElements": 2,
    "first": true,
    "last": true,
    "size": 1000,
    "content": [
        {
            "chargeNumber": "883677-1",
            "prisonerNumber": "G4946VC",
            "gender": "MALE",
            "incidentDetails": {
                "locationId": 84722,
                "dateTimeOfIncident": "2014-01-27T12:55:00",
                "dateTimeOfDiscovery": "2014-01-27T12:55:00",
                "handoverDeadline": "2014-01-29T12:55:00"
            },
            "isYouthOffender": false,
            "incidentRole": {},
            "offenceDetails": {
                "offenceCode": 0,
                "offenceRule": {
                    "paragraphNumber": "51:9",
                    "paragraphDescription": "Administers a controlled drug to himself or fails to prevent the administration of a controlled drug to him by another person (but subject to rule 52)"
                },
                "protectedCharacteristics": []
            },
            "incidentStatement": {
                "statement": "MMyCEgyUflzosGtDyhRSuitkhsQdZlFlOfZdtydkHMvpJWVHqNTZWnJuFPgdREGuQZRmFWoFuMHDFOsYFkMMyCEgyUflzosGtDyhRSuitkhsQdZlFlOfZdtydkHMvpJWVHqNTZWnJuFPgdREGuQZRmFWoFuMHDFOsYFk",
                "completed": true
            },
            "createdByUserId": "ZQZ33D",
            "createdDateTime": "2014-01-27T19:04:00",
            "status": "CHARGE_PROVED",
            "damages": [],
            "evidence": [],
            "witnesses": [
                {
                    "code": "OTHER_PERSON",
                    "firstName": "DOPISKAS",
                    "lastName": "COBY",
                    "reporter": "FQD46T"
                }
            ],
            "hearings": [
                {
                    "id": 1162607,
                    "locationId": 84692,
                    "dateTimeOfHearing": "2014-02-05T14:02:00",
                    "oicHearingType": "GOV_ADULT",
                    "outcome": {
                        "id": 1074457,
                        "adjudicator": "TQG39J",
                        "code": "ADJOURN",
                        "reason": "OTHER",
                        "details": "created via migration DmZSbYuVhDmZSbYuV",
                        "plea": "NOT_ASKED"
                    },
                    "agencyId": "BMI"
                },
                {
                    "id": 1162609,
                    "locationId": 84692,
                    "dateTimeOfHearing": "2014-02-26T09:30:00",
                    "oicHearingType": "INAD_ADULT",
                    "outcome": {
                        "id": 1074459,
                        "adjudicator": "",
                        "code": "COMPLETE",
                        "details": "nVtpfxNnVtpfxN",
                        "plea": "GUILTY"
                    },
                    "agencyId": "BMI"
                }
            ],
            "disIssueHistory": [],
            "outcomes": [
                {
                    "hearing": {
                        "id": 1162607,
                        "locationId": 84692,
                        "dateTimeOfHearing": "2014-02-05T14:02:00",
                        "oicHearingType": "GOV_ADULT",
                        "outcome": {
                            "id": 1074457,
                            "adjudicator": "TQG39J",
                            "code": "ADJOURN",
                            "reason": "OTHER",
                            "details": "created via migration DmZSbYuVhDmZSbYuV",
                            "plea": "NOT_ASKED"
                        },
                        "agencyId": "BMI"
                    }
                },
                {
                    "hearing": {
                        "id": 1162609,
                        "locationId": 84692,
                        "dateTimeOfHearing": "2014-02-26T09:30:00",
                        "oicHearingType": "INAD_ADULT",
                        "outcome": {
                            "id": 1074459,
                            "adjudicator": "",
                            "code": "COMPLETE",
                            "details": "nVtpfxNnVtpfxN",
                            "plea": "GUILTY"
                        },
                        "agencyId": "BMI"
                    },
                    "outcome": {
                        "outcome": {
                            "id": 784715,
                            "code": "CHARGE_PROVED",
                            "details": "nVtpfxNnVtpfxN",
                            "canRemove": true
                        }
                    }
                }
            ],
            "punishments": [
                {
                    "id": 1330016,
                    "type": "ADDITIONAL_DAYS",
                    "schedule": {
                        "days": 8,
                        "duration": 8,
                        "measurement": "DAYS"
                    },
                    "canRemove": true,
                    "canEdit": true,
                    "rehabilitativeActivities": []
                }
            ],
            "punishmentComments": [],
            "outcomeEnteredInNomis": false,
            "overrideAgencyId": "KMI",
            "originatingAgencyId": "BMI",
            "transferableActionsAllowed": true,
            "linkedChargeNumbers": [],
            "canActionFromHistory": false
        },
        {
            "chargeNumber": "600261-1",
            "prisonerNumber": "G4946VC",
            "gender": "MALE",
            "incidentDetails": {
                "locationId": 723,
                "dateTimeOfIncident": "2011-03-17T00:00:00",
                "dateTimeOfDiscovery": "2011-03-17T00:00:00",
                "handoverDeadline": "2011-03-19T00:00:00"
            },
            "isYouthOffender": false,
            "incidentRole": {},
            "offenceDetails": {
                "offenceCode": 0,
                "offenceRule": {
                    "paragraphNumber": "51:12A",
                    "paragraphDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items"
                },
                "protectedCharacteristics": []
            },
            "incidentStatement": {
                "statement": "qKylvVopqKylvVo",
                "completed": true
            },
            "createdByUserId": "XTAG",
            "createdDateTime": "2011-03-17T00:00:00",
            "status": "CHARGE_PROVED",
            "damages": [],
            "evidence": [
                {
                    "code": "OTHER",
                    "details": "445",
                    "reporter": "XTAG"
                }
            ],
            "witnesses": [],
            "hearings": [
                {
                    "id": 378735,
                    "locationId": 723,
                    "dateTimeOfHearing": "2011-04-18T00:00:00",
                    "oicHearingType": "GOV_ADULT",
                    "outcome": {
                        "id": 363584,
                        "adjudicator": "",
                        "code": "COMPLETE",
                        "details": "ZYeUSKJWZYeUSKJ",
                        "plea": "GUILTY"
                    },
                    "agencyId": "ACI"
                }
            ],
            "disIssueHistory": [],
            "outcomes": [
                {
                    "hearing": {
                        "id": 378735,
                        "locationId": 723,
                        "dateTimeOfHearing": "2011-04-18T00:00:00",
                        "oicHearingType": "GOV_ADULT",
                        "outcome": {
                            "id": 363584,
                            "adjudicator": "",
                            "code": "COMPLETE",
                            "details": "ZYeUSKJWZYeUSKJ",
                            "plea": "GUILTY"
                        },
                        "agencyId": "ACI"
                    },
                    "outcome": {
                        "outcome": {
                            "id": 309326,
                            "code": "CHARGE_PROVED",
                            "details": "ZYeUSKJWZYeUSKJ",
                            "canRemove": true
                        }
                    }
                }
            ],
            "punishments": [
                {
                    "id": 633259,
                    "type": "ADDITIONAL_DAYS",
                    "schedule": {
                        "days": 21,
                        "duration": 21,
                        "measurement": "DAYS"
                    },
                    "canRemove": true,
                    "canEdit": true,
                    "rehabilitativeActivities": []
                }
            ],
            "punishmentComments": [
                {
                    "id": 560470,
                    "comment": "wAYQVqlzwAYQVql",
                    "createdByUserId": "XTAG",
                    "dateTime": "2023-11-21T15:18:00.943929"
                }
            ],
            "outcomeEnteredInNomis": false,
            "originatingAgencyId": "ACI",
            "linkedChargeNumbers": [],
            "canActionFromHistory": false
        }
    ],
    "number": 0,
    "sort": {
        "unsorted": false,
        "sorted": true,
        "empty": false
    },
    "empty": false
}"""
