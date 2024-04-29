package uk.gov.justice.digital.hmpps.adjustments.api.wiremock

data class AdjudicationResponses(
  val searchResponse: String = """
  {
   "results": [
     {
       "adjudicationNumber": 1468919,
       "reportTime": "2017-01-18T11:02:00",
       "agencyIncidentId": 1447062,
       "agencyId": "KMI",
       "partySeq": 1,
       "adjudicationCharges": [
         {
           "oicChargeId": "1468919/1",
           "offenceCode": "51:21",
           "offenceDescription": "Intentionally fails to work properly or, being required to work, refuses to do so",
           "findingCode": "PROVED"
         }
       ]
     },
     {
       "adjudicationNumber": 1270334,
       "reportTime": "2016-03-03T15:59:00",
       "agencyIncidentId": 1252122,
       "agencyId": "BCI",
       "partySeq": 1,
       "adjudicationCharges": [
         {
           "oicChargeId": "1270334/1",
           "offenceCode": "51:20",
           "offenceDescription": "Uses threatening, abusive or insulting words or behaviour",
           "findingCode": "APPEAL"
         }
       ]
     },
     {
       "adjudicationNumber": 998947,
       "reportTime": "2014-10-16T06:47:00",
       "agencyIncidentId": 984422,
       "agencyId": "DNI",
       "partySeq": 1,
       "adjudicationCharges": [
         {
           "oicChargeId": "998947/1",
           "offenceCode": "51:12A",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "findingCode": "PROVED"
         },
         {
           "oicChargeId": "998947/1",
           "offenceCode": "51:12A",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "findingCode": "REF_POLICE"
         }
       ]
     },
     {
       "adjudicationNumber": 998946,
       "reportTime": "2014-10-16T06:44:00",
       "agencyIncidentId": 984421,
       "agencyId": "DNI",
       "partySeq": 1,
       "adjudicationCharges": [
         {
           "oicChargeId": "998946/1",
           "offenceCode": "51:12A",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "findingCode": "PROVED"
         },
         {
           "oicChargeId": "998946/1",
           "offenceCode": "51:12A",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "findingCode": "REF_POLICE"
         }
       ]
     },
     {
       "adjudicationNumber": 998945,
       "reportTime": "2014-10-16T06:41:00",
       "agencyIncidentId": 984420,
       "agencyId": "DNI",
       "partySeq": 1,
       "adjudicationCharges": [
         {
           "oicChargeId": "998945/1",
           "offenceCode": "51:12A",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "findingCode": "PROVED"
         },
         {
           "oicChargeId": "998945/1",
           "offenceCode": "51:12A",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "findingCode": "REF_POLICE"
         }
       ]
     },
     {
       "adjudicationNumber": 104837,
       "reportTime": "2007-12-09T00:00:00",
       "agencyIncidentId": 103790,
       "agencyId": "WOI",
       "partySeq": 1,
       "adjudicationCharges": [
         {
           "oicChargeId": "104837/1",
           "offenceCode": "51:20",
           "offenceDescription": "Uses threatening, abusive or insulting words or behaviour",
           "findingCode": "DISMISSED"
         }
       ]
     },
     {
       "adjudicationNumber": 104838,
       "reportTime": "2003-04-01T00:00:00",
       "agencyIncidentId": 103791,
       "agencyId": "WRI",
       "partySeq": 1,
       "adjudicationCharges": [
         {
           "oicChargeId": "104838/1",
           "offenceCode": "51:12A",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "findingCode": "PROVED"
         }
       ]
     },
     {
       "adjudicationNumber": 104840,
       "reportTime": "2001-09-03T00:00:00",
       "agencyIncidentId": 103793,
       "agencyId": "MRI",
       "partySeq": 1,
       "adjudicationCharges": [
         {
           "oicChargeId": "104840/1",
           "offenceCode": "51:4",
           "offenceDescription": "Fights with any person;",
           "findingCode": "QUASHED"
         }
       ]
     },
     {
       "adjudicationNumber": 104839,
       "reportTime": "2001-01-09T00:00:00",
       "agencyIncidentId": 103792,
       "agencyId": "FBI",
       "partySeq": 1,
       "adjudicationCharges": [
         {
           "oicChargeId": "104839/1",
           "offenceCode": "51:1N",
           "offenceDescription": "Commits any assault - assault on non prison officer member of staff",
           "findingCode": "DISMISSED"
         }
       ]
     },
     {
       "adjudicationNumber": 104841,
       "reportTime": "2000-06-19T00:00:00",
       "agencyIncidentId": 103794,
       "agencyId": "PNI",
       "partySeq": 1,
       "adjudicationCharges": [
         {
           "oicChargeId": "104841/1",
           "offenceCode": "51:9",
           "offenceDescription": "Administers a controlled drug to himself or fails to prevent the administration of a controlled drug to him by another person (but subject to rule 52)",
           "findingCode": "PROVED"
         }
       ]
     }
   ],
   "offences": [
     {
       "id": "9",
       "code": "51:9",
       "description": "Administers a controlled drug to himself or fails to prevent the administration of a controlled drug to him by another person (but subject to rule 52)"
     },
     {
       "id": "81",
       "code": "51:1N",
       "description": "Commits any assault - assault on non prison officer member of staff"
     },
     {
       "id": "5",
       "code": "51:4",
       "description": "Fights with any person;"
     },
     {
       "id": "88",
       "code": "51:12A",
       "description": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items"
     },
     {
       "id": "17",
       "code": "51:21",
       "description": "Intentionally fails to work properly or, being required to work, refuses to do so"
     },
     {
       "id": "16",
       "code": "51:20",
       "description": "Uses threatening, abusive or insulting words or behaviour"
     }
   ],
   "agencies": [
     {
       "agencyId": "FBI",
       "description": "Forest Bank (HMP & YOI)",
       "agencyType": "INST",
       "active": true
     },
     {
       "agencyId": "BCI",
       "description": "Buckley Hall (HMP)",
       "agencyType": "INST",
       "active": true
     },
     {
       "agencyId": "DNI",
       "description": "Doncaster (HMP)",
       "agencyType": "INST",
       "active": true
     },
     {
       "agencyId": "KMI",
       "description": "Kirkham (HMP)",
       "agencyType": "INST",
       "active": true
     },
     {
       "agencyId": "MRI",
       "description": "Manchester (HMP)",
       "agencyType": "INST",
       "active": true
     },
     {
       "agencyId": "PNI",
       "description": "Preston (HMP)",
       "agencyType": "INST",
       "active": true
     },
     {
       "agencyId": "WOI",
       "description": "Wolds (HMP)",
       "agencyType": "INST",
       "active": false
     },
     {
       "agencyId": "WRI",
       "description": "Whitemoor (HMP)",
       "agencyType": "INST",
       "active": true
     }
   ]
 }
  """.trimIndent(),

  val `adjudicationResponses`: List<Pair<String, String>> = listOf(
    "1468919" to """
   {
     "adjudicationNumber": 1468919,
     "incidentTime": "2017-01-18T11:02:00",
     "establishment": "Kirkham (HMP)",
     "interiorLocation": "Induction",
     "incidentDetails": "llxtkKcUnoOLEMQKngDzNNPHPqTMqdNZsZfKjtaXZTFalqiweDsKjAZAbQFbopufNuWoFriMAIazDyEttLWaemaUplmYjUCmObPrhRIeMSJnzdtVbcCScVzllxtkKcUnoOLEMQKngDzNNPHPqTMqdNZsZfKjtaXZTFalqiweDsKjAZAbQFbopufNuWoFriMAIazDyEttLWaemaUplmYjUCmObPrhRIeMSJnzdtVbcCScVz",
     "reportNumber": 1447062,
     "reportType": "Governor's Report",
     "reporterFirstName": "ULVNECHE",
     "reporterLastName": "MELVAN",
     "reportTime": "2017-01-18T11:02:00",
     "hearings": [
       {
         "oicHearingId": 1931177,
         "hearingType": "Governor's Hearing Adult",
         "hearingTime": "2017-01-19T09:43:00",
         "establishment": "Kirkham (HMP)",
         "location": "Seg Adjudication Room",
         "otherRepresentatives": "WntdxWntdx",
         "results": [
           {
             "oicOffenceCode": "51:21",
             "offenceType": "Prison Rule 51",
             "offenceDescription": "Intentionally fails to work properly or, being required to work, refuses to do so",
             "plea": "Guilty",
             "finding": "Charge Proved",
             "sanctions": [
               {
                 "sanctionType": "Additional Days Added",
                 "sanctionDays": 10,
                 "effectiveDate": "2023-10-03T00:00:00",
                 "status": "Prospective",
                 "statusDate": "2023-10-03T00:00:00",
                 "sanctionSeq": 13
               }
             ]
           }
         ]
       }
     ]
   }
    """.trimIndent(),

    "998945" to """
   {
   "adjudicationNumber": 998945,
   "incidentTime": "2014-10-16T06:41:00",
   "establishment": "Doncaster (HMP)",
   "interiorLocation": "Recreation  Rm",
   "incidentDetails": "flVghKnvWbTuwhQQhRuHxctYWCGpFOVflVghKnvWbTuwhQQhRuHxctYWCGpFOV",
   "reportNumber": 984420,
   "reportType": "Governor's Report",
   "reporterFirstName": "UHOYNEKE",
   "reporterLastName": "AMBENTINO",
   "reportTime": "2014-10-16T06:41:00",
   "hearings": [
     {
       "oicHearingId": 1289216,
       "hearingType": "Independent Adjudicator Hearing Adult",
       "hearingTime": "2014-11-24T13:30:00",
       "establishment": "Doncaster (HMP)",
       "location": "Adjudication Room",
       "otherRepresentatives": "nMKnMK",
       "results": [
         {
           "oicOffenceCode": "51:12A",
           "offenceType": "Prison Rule 51",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "plea": "Guilty",
           "finding": "Charge Proved",
           "sanctions": [
             {
               "sanctionType": "Forfeiture of Privileges",
               "sanctionDays": 21,
               "effectiveDate": "2014-11-24T00:00:00",
               "status": "Immediate",
               "statusDate": "2014-11-24T00:00:00",
               "sanctionSeq": 7
             }
           ]
         }
       ]
     },
     {
       "oicHearingId": 1262641,
       "hearingType": "Governor's Hearing Adult",
       "hearingTime": "2014-10-16T09:57:00",
       "establishment": "Doncaster (HMP)",
       "location": "Adjudication Room",
       "otherRepresentatives": "cfvcfv",
       "results": [
         {
           "oicOffenceCode": "51:12A",
           "offenceType": "Prison Rule 51",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "plea": "Not guilty",
           "finding": "Referred to Police",
           "sanctions": []
         }
       ]
     },
     {
       "oicHearingId": 1267076,
       "hearingType": "Governor's Hearing Adult",
       "hearingTime": "2014-10-23T14:17:00",
       "establishment": "Doncaster (HMP)",
       "location": "Adjudication Room",
       "otherRepresentatives": "XAEeXAE",
       "comment": "oeyoey",
       "results": []
     }
   ]
 }
    """.trimIndent(),

    "998947" to """
  {
   "adjudicationNumber": 998947,
   "incidentTime": "2014-10-16T06:47:00",
   "establishment": "Doncaster (HMP)",
   "interiorLocation": "Servery",
   "incidentDetails": "WnZJMuhdyCObwatxTdxAIVJIwDIyWnZJMuhdyCObwatxTdxAIVJIwDIy",
   "reportNumber": 984422,
   "reportType": "Governor's Report",
   "reporterFirstName": "UHOYNEKE",
   "reporterLastName": "AMBENTINO",
   "reportTime": "2014-10-16T06:47:00",
   "hearings": [
     {
       "oicHearingId": 1289218,
       "hearingType": "Independent Adjudicator Hearing Adult",
       "hearingTime": "2014-11-24T13:31:00",
       "establishment": "Doncaster (HMP)",
       "location": "Adjudication Room",
       "otherRepresentatives": "KEKKEK",
       "results": [
         {
           "oicOffenceCode": "51:12A",
           "offenceType": "Prison Rule 51",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "plea": "Guilty",
           "finding": "Charge Proved",
           "sanctions": [
             {
               "sanctionType": "Additional Days Added",
               "sanctionDays": 21,
               "effectiveDate": "2014-11-24T00:00:00",
               "status": "Immediate",
               "statusDate": "2014-11-24T00:00:00",
               "sanctionSeq": 9
             }
           ]
         }
       ]
     },
     {
       "oicHearingId": 1262643,
       "hearingType": "Governor's Hearing Adult",
       "hearingTime": "2014-10-16T09:58:00",
       "establishment": "Doncaster (HMP)",
       "location": "Adjudication Room",
       "otherRepresentatives": "WcGWcG",
       "results": [
         {
           "oicOffenceCode": "51:12A",
           "offenceType": "Prison Rule 51",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "plea": "Not guilty",
           "finding": "Referred to Police",
           "sanctions": []
         }
       ]
     },
     {
       "oicHearingId": 1267078,
       "hearingType": "Governor's Hearing Adult",
       "hearingTime": "2014-10-23T14:18:00",
       "establishment": "Doncaster (HMP)",
       "location": "Adjudication Room",
       "otherRepresentatives": "yYFPyYF",
       "comment": "cKscKs",
       "results": []
     }
   ]
 }
    """.trimIndent(),

    "1270334" to """
  {
   "adjudicationNumber": 1270334,
   "incidentTime": "2016-03-03T15:59:00",
   "establishment": "Buckley Hall (HMP)",
   "interiorLocation": "Staff Mess",
   "incidentDetails": "VjswhIBPgsNTXqKJsilwXwwaJzRBrQFQGNHhNgBWqnPboomXlVUoBDcfjKyZZGfnGUAlWMfiVBVjswhIBPgsNTXqKJsilwXwwaJzRBrQFQGNHhNgBWqnPboomXlVUoBDcfjKyZZGfnGUAlWMfiVB",
   "reportNumber": 1252122,
   "reportType": "Governor's Report",
   "reporterFirstName": "OLBDALIAN",
   "reporterLastName": "JOYCESA",
   "reportTime": "2016-03-03T15:59:00",
   "hearings": [
     {
       "oicHearingId": 1647990,
       "hearingType": "Governor's Hearing Adult",
       "hearingTime": "2016-03-04T09:00:00",
       "establishment": "Buckley Hall (HMP)",
       "location": "Adjudication Room",
       "results": []
     },
     {
       "oicHearingId": 1681878,
       "hearingType": "Governor's Hearing Adult",
       "hearingTime": "2016-04-14T09:00:00",
       "establishment": "Buckley Hall (HMP)",
       "location": "Adjudication Room",
       "heardByFirstName": "ANMUALRICHARD",
       "heardByLastName": "DONOPHER",
       "comment": "ltzltz",
       "results": [
         {
           "oicOffenceCode": "51:20",
           "offenceType": "Prison Rule 51",
           "offenceDescription": "Uses threatening, abusive or insulting words or behaviour",
           "plea": "Not guilty",
           "finding": "Quashed on Appeal",
           "sanctions": [
             {
               "sanctionType": "Forfeiture of Privileges",
               "sanctionDays": 7,
               "effectiveDate": "2016-04-15T00:00:00",
               "status": "Quashed",
               "statusDate": "2016-04-15T00:00:00",
               "comment": "pvNuAMpvNuA",
               "sanctionSeq": 10
             },
             {
               "sanctionType": "Forfeiture of Privileges",
               "sanctionDays": 7,
               "effectiveDate": "2016-04-15T00:00:00",
               "status": "Quashed",
               "statusDate": "2016-04-15T00:00:00",
               "comment": "MKFGiHMKFGi",
               "sanctionSeq": 11
             },
             {
               "sanctionType": "Forfeiture of Privileges",
               "sanctionDays": 7,
               "effectiveDate": "2016-04-15T00:00:00",
               "status": "Quashed",
               "statusDate": "2016-04-15T00:00:00",
               "comment": "ouo",
               "sanctionSeq": 12
             }
           ]
         }
       ]
     }
   ]
 }
    """.trimIndent(),

    "104837" to """
  {
   "adjudicationNumber": 104837,
   "incidentTime": "2007-12-09T00:00:00",
   "establishment": "Wolds (HMP)",
   "interiorLocation": "Wolds (HMP)",
   "incidentDetails": "ZDFTPdPYZDFTPdP",
   "reportNumber": 103790,
   "reportType": "Miscellaneous",
   "reporterFirstName": "XTAG",
   "reporterLastName": "XTAG",
   "reportTime": "2007-12-09T00:00:00",
   "hearings": [
     {
       "oicHearingId": 105775,
       "hearingType": "Governor's Hearing",
       "hearingTime": "2007-12-13T00:00:00",
       "establishment": "Wolds (HMP)",
       "location": "Wolds (HMP)",
       "otherRepresentatives": "jXLkjvKLTMgezdjXLkjvKLTMgez",
       "comment": "uYYMvlLZuYYMvlL",
       "results": [
         {
           "oicOffenceCode": "51:20",
           "offenceType": "Prison Rule 51",
           "offenceDescription": "Uses threatening, abusive or insulting words or behaviour",
           "plea": "Not guilty",
           "finding": "Discharged",
           "sanctions": []
         }
       ]
     }
   ]
 }
    """.trimIndent(),

    "104838" to """
  {
   "adjudicationNumber": 104838,
   "incidentTime": "2003-04-01T00:00:00",
   "establishment": "Whitemoor (HMP)",
   "interiorLocation": "Whitemoor (HMP)",
   "incidentDetails": "ATFStWzSATFStWz",
   "reportNumber": 103791,
   "reportType": "Miscellaneous",
   "reporterFirstName": "XTAG",
   "reporterLastName": "XTAG",
   "reportTime": "2003-04-01T00:00:00",
   "hearings": [
     {
       "oicHearingId": 105776,
       "hearingType": "Governor's Hearing",
       "hearingTime": "2003-04-03T00:00:00",
       "establishment": "Whitemoor (HMP)",
       "location": "Whitemoor (HMP)",
       "otherRepresentatives": "YwLiCLyKidFsKRYwLiCLyKidFsK",
       "comment": "XKTlCeTaXKTlCeT",
       "results": [
         {
           "oicOffenceCode": "51:12A",
           "offenceType": "Prison Rule 51",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "plea": "Guilty",
           "finding": "Charge Proved",
           "sanctions": [
             {
               "sanctionType": "Stoppage of Earnings (%)",
               "sanctionDays": 7,
               "compensationAmount": 100,
               "effectiveDate": "2003-04-03T00:00:00",
               "status": "Immediate",
               "comment": "dlxpnnnEdlxpnnn",
               "sanctionSeq": 1
             },
             {
               "sanctionType": "Forfeiture of Privileges",
               "sanctionDays": 7,
               "compensationAmount": 0,
               "effectiveDate": "2003-04-03T00:00:00",
               "status": "Immediate",
               "comment": "MfaqZvuWMfaqZvu",
               "sanctionSeq": 2
             },
             {
               "sanctionType": "Canteen Facilities",
               "effectiveDate": "2003-04-03T00:00:00",
               "status": "Immediate",
               "comment": "OeHpoHHbqImLluhUOeHpoHHbqImLluh",
               "sanctionSeq": 3
             },
             {
               "sanctionType": "Cellular Confinement",
               "sanctionDays": 7,
               "compensationAmount": 0,
               "effectiveDate": "2003-04-03T00:00:00",
               "status": "Suspended",
               "comment": "OrZpTGuZOrZpTGu",
               "sanctionSeq": 4
             }
           ]
         }
       ]
     }
   ]
 }
    """.trimIndent(),

    "104839" to """
  {
   "adjudicationNumber": 104839,
   "incidentTime": "2001-01-09T00:00:00",
   "establishment": "Forest Bank (HMP & YOI)",
   "interiorLocation": "Forest Bank (HMP & YOI)",
   "incidentDetails": "fQkiVFjzfQkiVFj",
   "reportNumber": 103792,
   "reportType": "Miscellaneous",
   "reporterFirstName": "XTAG",
   "reporterLastName": "XTAG",
   "reportTime": "2001-01-09T00:00:00",
   "hearings": [
     {
       "oicHearingId": 105777,
       "hearingType": "Governor's Hearing",
       "hearingTime": "2001-01-10T00:00:00",
       "establishment": "Forest Bank (HMP & YOI)",
       "location": "Forest Bank (HMP & YOI)",
       "otherRepresentatives": "MVqNJbrPLkVvMVqNJbrPLkV",
       "comment": "eNJZPfcteNJZPfc",
       "results": [
         {
           "oicOffenceCode": "51:1N",
           "offenceType": "Prison Rule 51",
           "offenceDescription": "Commits any assault - assault on non prison officer member of staff",
           "plea": "Not guilty",
           "finding": "Discharged",
           "sanctions": []
         }
       ]
     }
   ]
 }
    """.trimIndent(),

    "998946" to """
  {
   "adjudicationNumber": 998946,
   "incidentTime": "2014-10-16T06:44:00",
   "establishment": "Doncaster (HMP)",
   "interiorLocation": "Recreation  Rm",
   "incidentDetails": "QXFKDFgKgsbCMgUHjyGBGxtcCocQXFKDFgKgsbCMgUHjyGBGxtcCoc",
   "reportNumber": 984421,
   "reportType": "Governor's Report",
   "reporterFirstName": "UHOYNEKE",
   "reporterLastName": "AMBENTINO",
   "reportTime": "2014-10-16T06:44:00",
   "hearings": [
     {
       "oicHearingId": 1289217,
       "hearingType": "Independent Adjudicator Hearing Adult",
       "hearingTime": "2014-11-24T13:30:00",
       "establishment": "Doncaster (HMP)",
       "location": "Adjudication Room",
       "otherRepresentatives": "HPqHPq",
       "results": [
         {
           "oicOffenceCode": "51:12A",
           "offenceType": "Prison Rule 51",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "plea": "Guilty",
           "finding": "Charge Proved",
           "sanctions": [
             {
               "sanctionType": "Additional Days Added",
               "sanctionDays": 21,
               "effectiveDate": "2014-11-24T00:00:00",
               "status": "Immediate",
               "statusDate": "2014-11-24T00:00:00",
               "sanctionSeq": 8
             }
           ]
         }
       ]
     },
     {
       "oicHearingId": 1262642,
       "hearingType": "Governor's Hearing Adult",
       "hearingTime": "2014-10-16T09:57:00",
       "establishment": "Doncaster (HMP)",
       "location": "Adjudication Room",
       "otherRepresentatives": "LhYLhY",
       "results": [
         {
           "oicOffenceCode": "51:12A",
           "offenceType": "Prison Rule 51",
           "offenceDescription": "Has in his possession - (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have - possession of unauthorised items",
           "plea": "Not guilty",
           "finding": "Referred to Police",
           "sanctions": []
         }
       ]
     },
     {
       "oicHearingId": 1267077,
       "hearingType": "Governor's Hearing Adult",
       "hearingTime": "2014-10-23T14:17:00",
       "establishment": "Doncaster (HMP)",
       "location": "Adjudication Room",
       "otherRepresentatives": "TjysTjy",
       "comment": "SKFSKF",
       "results": []
     }
   ]
 }
    """.trimIndent(),

    "104841" to """
  {
   "adjudicationNumber": 104841,
   "incidentTime": "2000-06-19T00:00:00",
   "establishment": "Preston (HMP)",
   "interiorLocation": "Preston (HMP)",
   "incidentDetails": "ELRFxDTkELRFxDT",
   "reportNumber": 103794,
   "reportType": "Miscellaneous",
   "reporterFirstName": "XTAG",
   "reporterLastName": "XTAG",
   "reportTime": "2000-06-19T00:00:00",
   "hearings": [
     {
       "oicHearingId": 105779,
       "hearingType": "Governor's Hearing",
       "hearingTime": "2000-07-25T00:00:00",
       "establishment": "Preston (HMP)",
       "location": "Preston (HMP)",
       "otherRepresentatives": "YKGHzkGyxUWYYCYKGHzkGyxUWYY",
       "comment": "KVJtbKuDKVJtbKu",
       "results": [
         {
           "oicOffenceCode": "51:9",
           "offenceType": "Prison Rule 51",
           "offenceDescription": "Administers a controlled drug to himself or fails to prevent the administration of a controlled drug to him by another person (but subject to rule 52)",
           "plea": "Guilty",
           "finding": "Charge Proved",
           "sanctions": [
             {
               "sanctionType": "Stoppage of Earnings (amount)",
               "sanctionDays": 7,
               "compensationAmount": 3,
               "effectiveDate": "2000-07-25T00:00:00",
               "status": "Immediate",
               "comment": "oheiHhcAoheiHhc",
               "sanctionSeq": 5
             },
             {
               "sanctionType": "Additional Days Added",
               "sanctionDays": 7,
               "compensationAmount": 0,
               "effectiveDate": "2009-10-18T00:00:00",
               "status": "Prospective",
               "comment": "XrxQVLIUXrxQVLI",
               "sanctionSeq": 6
             }
           ]
         }
       ]
     }
   ]
 }
    """.trimIndent(),

    "104840" to """
 {
   "adjudicationNumber": 104840,
   "incidentTime": "2001-09-03T00:00:00",
   "establishment": "Manchester (HMP)",
   "interiorLocation": "Manchester (HMP)",
   "incidentDetails": "eBKnWIlgeBKnWIl",
   "reportNumber": 103793,
   "reportType": "Miscellaneous",
   "reporterFirstName": "XTAG",
   "reporterLastName": "XTAG",
   "reportTime": "2001-09-03T00:00:00",
   "hearings": [
     {
       "oicHearingId": 105778,
       "hearingType": "Governor's Hearing",
       "hearingTime": "2001-09-08T00:00:00",
       "establishment": "Manchester (HMP)",
       "location": "Manchester (HMP)",
       "otherRepresentatives": "EqveqgjiRfYfwPEqveqgjiRfYfw",
       "comment": "FOwiFsoYFOwiFso",
       "results": [
         {
           "oicOffenceCode": "51:4",
           "offenceType": "Prison Rule 51",
           "offenceDescription": "Fights with any person;",
           "plea": "Not guilty",
           "finding": "Quashed",
           "sanctions": []
         }
       ]
     }
   ]
 }
    """.trimIndent(),
  ),
)
