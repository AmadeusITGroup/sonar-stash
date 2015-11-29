package org.sonar.plugins.stash.issue.collector;

public class DiffReportSample {

  public static String baseReport = "{\"diffs\": ["
      + "        {"
      + "            \"source\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "                ],"
      + "                \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "            \"destination\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "               ],"
      + "               \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "            \"hunks\": ["
      + "                {"
      + "                    \"segments\": ["
      + "                        {"
      + "                            \"type\": \"CONTEXT\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 10,"
      + "                                    \"destination\": 20,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                    \"commentIds\": ["
      + "                                     12345"
      + "                                    ]"
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"REMOVED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 20,"
      + "                                    \"destination\": 30,"
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"ADDED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 30,"
      + "                                    \"destination\": 40,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"CONTEXT\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                   \"source\": 40,"
      + "                                    \"destination\": 50,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                    \"commentIds\": ["
      + "                                        54321"
      + "                                    ]"
      + "                               }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"REMOVED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 50,"
      + "                                    \"destination\": 60,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"ADDED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 60,"
      + "                                    \"destination\": 70,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                    ],"
      + "                }"
      + "            ],"
      + "            \"lineComments\": ["
      + "                {"
      + "                    \"id\": 12345,"
      + "                    \"text\": \"Test comment\","
      + "                    \"version\": 1,"
      + "                    \"author\": "
      + "                       {"
      + "                         \"id\": 12345,"
      + "                         \"name\": \"SonarQube\","
      + "                         \"slug\": \"sonarqube\","
      + "                         \"email\": \"sq@email.com\","
      + "                       },"
      + "                },"
      + "                {"
      + "                    \"id\": 54321,"
      + "                    \"text\": \"Test comment 2\","
      + "                    \"version\": 1,"
      + "                    \"author\": "
      + "                       {"
      + "                         \"id\": 54321,"
      + "                         \"name\": \"SonarQube2\","
      + "                         \"slug\": \"sonarqube2\","
      + "                         \"email\": \"sq2@email.com\","
      + "                       },"
      + "                }"
      + "            ]"
      + "        }"
      + "    ]"
      + "}";
  
  public static String baseReportWithFileComments = "{\"diffs\": ["
      + "        {"
      + "            \"source\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "                ],"
      + "                \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "            \"destination\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "               ],"
      + "               \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "            \"hunks\": ["
      + "                {"
      + "                    \"segments\": ["
      + "                        {"
      + "                            \"type\": \"CONTEXT\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 10,"
      + "                                    \"destination\": 20,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                    \"commentIds\": ["
      + "                                     12345"
      + "                                    ]"
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"REMOVED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 20,"
      + "                                    \"destination\": 30,"
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"ADDED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 30,"
      + "                                    \"destination\": 40,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"CONTEXT\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                   \"source\": 40,"
      + "                                    \"destination\": 50,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                    \"commentIds\": ["
      + "                                        54321"
      + "                                    ]"
      + "                               }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"REMOVED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 50,"
      + "                                    \"destination\": 60,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"ADDED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 60,"
      + "                                    \"destination\": 70,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                    ],"
      + "                }"
      + "            ],"
      + "            \"lineComments\": ["
      + "                {"
      + "                    \"id\": 12345,"
      + "                    \"text\": \"Test comment\","
      + "                    \"version\": 1,"
      + "                    \"author\": "
      + "                       {"
      + "                         \"id\": 12345,"
      + "                         \"name\": \"SonarQube\","
      + "                         \"slug\": \"sonarqube\","
      + "                         \"email\": \"sq@email.com\","
      + "                       },"
      + "                },"
      + "                {"
      + "                    \"id\": 54321,"
      + "                    \"text\": \"Test comment 2\","
      + "                    \"version\": 1,"
      + "                    \"author\": "
      + "                       {"
      + "                         \"id\": 54321,"
      + "                         \"name\": \"SonarQube2\","
      + "                         \"slug\": \"sonarqube2\","
      + "                         \"email\": \"sq2@email.com\","
      + "                       },"
      + "                }"
      + "            ]"
      + "            \"fileComments\": ["
      + "                {"
      + "                    \"id\": 123456,"
      + "                    \"text\": \"Test File comment\","
      + "                    \"version\": 1,"
      + "                    \"author\": "
      + "                       {"
      + "                         \"id\": 12345,"
      + "                         \"name\": \"SonarQube\","
      + "                         \"slug\": \"sonarqube\","
      + "                         \"email\": \"sq@email.com\","
      + "                       },"
      + "                },"
      + "                {"
      + "                    \"id\": 654321,"
      + "                    \"text\": \"Test File comment 2\","
      + "                    \"version\": 1,"
      + "                    \"author\": "
      + "                       {"
      + "                         \"id\": 54321,"
      + "                         \"name\": \"SonarQube2\","
      + "                         \"slug\": \"sonarqube2\","
      + "                         \"email\": \"sq2@email.com\","
      + "                       },"
      + "                }"
      + "            ]"
      + "        }"
      + "    ]"
      + "}";
  
  public static String baseReportWithEmptyFileComments = "{\"diffs\": ["
      + "        {"
      + "            \"source\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "                ],"
      + "                \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "            \"destination\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "               ],"
      + "               \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "            \"hunks\": ["
      + "                {"
      + "                    \"segments\": ["
      + "                        {"
      + "                            \"type\": \"CONTEXT\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 10,"
      + "                                    \"destination\": 20,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                    \"commentIds\": ["
      + "                                     12345"
      + "                                    ]"
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"REMOVED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 20,"
      + "                                    \"destination\": 30,"
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"ADDED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 30,"
      + "                                    \"destination\": 40,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"CONTEXT\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                   \"source\": 40,"
      + "                                    \"destination\": 50,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                    \"commentIds\": ["
      + "                                        54321"
      + "                                    ]"
      + "                               }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"REMOVED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 50,"
      + "                                    \"destination\": 60,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"ADDED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 60,"
      + "                                    \"destination\": 70,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                    ],"
      + "                }"
      + "            ],"
      + "            \"lineComments\": ["
      + "                {"
      + "                    \"id\": 12345,"
      + "                    \"text\": \"Test comment\","
      + "                    \"version\": 1,"
      + "                    \"author\": "
      + "                       {"
      + "                         \"id\": 12345,"
      + "                         \"name\": \"SonarQube\","
      + "                         \"slug\": \"sonarqube\","
      + "                         \"email\": \"sq@email.com\","
      + "                       },"
      + "                },"
      + "                {"
      + "                    \"id\": 54321,"
      + "                    \"text\": \"Test comment 2\","
      + "                    \"version\": 1,"
      + "                    \"author\": "
      + "                       {"
      + "                         \"id\": 54321,"
      + "                         \"name\": \"SonarQube2\","
      + "                         \"slug\": \"sonarqube2\","
      + "                         \"email\": \"sq2@email.com\","
      + "                       },"
      + "                }"
      + "            ]"
      + "            \"fileComments\": []"
      + "        }"
      + "    ]"
      + "}";
  
  public static String emptyReport = "{\"diffs\": ["
      + "        {"
      + "            \"source\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "                ],"
      + "                \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "                \"destination\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "               ],"
      + "                \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "            \"hunks\": [],"
      + "            \"lineComments\": []"
      + "        }"
      + "    ]"
      + "}";
  
  public static String multipleFileReport = "{\"diffs\": ["
      + "        {"
      + "            \"source\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "                ],"
      + "                \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "            \"destination\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "               ],"
      + "               \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "            \"hunks\": ["
      + "                {"
      + "                    \"segments\": ["
      + "                        {"
      + "                            \"type\": \"CONTEXT\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 10,"
      + "                                    \"destination\": 20,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                    \"commentIds\": ["
      + "                                     12345"
      + "                                    ]"
      + "                                }"
      + "                            ],"
      + "                        }"
      + "                    ],"
      + "                }"
      + "            ],"
      + "            \"lineComments\": ["
      + "                {"
      + "                    \"id\": 12345,"
      + "                    \"text\": \"Test comment\","
      + "                    \"version\": 1,"
      + "                    \"author\": "
      + "                       {"
      + "                         \"id\": 12345,"
      + "                         \"name\": \"SonarQube\","
      + "                         \"slug\": \"sonarqube\","
      + "                         \"email\": \"sq@email.com\","
      + "                       },"
      + "                }"
      + "            ]"
      + "        },"
      + "        {"
      + "            \"source\": {"
      + "                \"components\": ["
      + "                    \"Test1.java\""
      + "                ],"
      + "                \"toString\": \"stash-plugin/Test1.java\""
      + "            },"
      + "            \"destination\": {"
      + "                \"components\": ["
      + "                    \"Test1.java\""
      + "               ],"
      + "               \"toString\": \"stash-plugin/Test1.java\""
      + "            },"
      + "            \"hunks\": ["
      + "                {"
      + "                    \"segments\": ["
      + "                        {"
      + "                            \"type\": \"ADDED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 20,"
      + "                                    \"destination\": 30,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                    \"commentIds\": []"
      + "                                }"
      + "                            ],"
      + "                        }"
      + "                    ],"
      + "                }"
      + "            ],"
      + "            \"lineComments\": []"
      + "        }"
      + "    ]"
      + "}";

  public static String baseReportWithNoComments = "{\"diffs\": ["
      + "        {"
      + "            \"source\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "                ],"
      + "                \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "            \"destination\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "               ],"
      + "               \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "            \"hunks\": ["
      + "                {"
      + "                    \"segments\": ["
      + "                        {"
      + "                            \"type\": \"CONTEXT\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 10,"
      + "                                    \"destination\": 20,"
      + "                                    \"line\": \"System.out.println(test);\""
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"REMOVED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 20,"
      + "                                    \"destination\": 30,"
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"ADDED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 30,"
      + "                                    \"destination\": 40,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"CONTEXT\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                   \"source\": 40,"
      + "                                    \"destination\": 50,"
      + "                                    \"line\": \"System.out.println(test);\""
      + "                               }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"REMOVED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 50,"
      + "                                    \"destination\": 60,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"ADDED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 60,"
      + "                                    \"destination\": 70,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                    ],"
      + "                }"
      + "            ],"
      + "            \"lineComments\": []"
      + "        }"
      + "    ]"
      + "}";

  public static String deletedFileReport = "{\"diffs\": ["
      + "        {"
      + "            \"source\": {"
      + "                \"components\": ["
      + "                    \"Test.java\""
      + "                ],"
      + "                \"toString\": \"stash-plugin/Test.java\""
      + "            },"
      + "            \"destination\": null,"
      + "            \"hunks\": ["
      + "                {"
      + "                    \"segments\": ["
      + "                        {"
      + "                            \"type\": \"REMOVED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 1,"
      + "                                    \"destination\": 0,"
      + "                                    \"line\": \"System.out.println(test);\""
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"REMOVED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 2,"
      + "                                    \"destination\": 0,"
      + "                                    \"line\": \"System.out.println(test);\""
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"REMOVED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 3,"
      + "                                    \"destination\": 0,"
      + "                                    \"line\": \"System.out.println(test);\""
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                    ],"
      + "                }"
      + "            ],"
      + "        }"
      + "        {"
      + "            \"source\": {"
      + "                \"components\": ["
      + "                    \"Test2.java\""
      + "                ],"
      + "                \"toString\": \"stash-plugin/Test2.java\""
      + "            },"
      + "            \"destination\": {"
      + "                \"components\": ["
      + "                    \"Test2.java\""
      + "               ],"
      + "               \"toString\": \"stash-plugin/Test2.java\""
      + "            },"
      + "            \"hunks\": ["
      + "                {"
      + "                    \"segments\": ["
      + "                        {"
      + "                            \"type\": \"CONTEXT\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 10,"
      + "                                    \"destination\": 20,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"REMOVED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 20,"
      + "                                    \"destination\": 30,"
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                        {"
      + "                            \"type\": \"ADDED\","
      + "                            \"lines\": ["
      + "                                {"
      + "                                    \"source\": 30,"
      + "                                    \"destination\": 40,"
      + "                                    \"line\": \"System.out.println(test);\","
      + "                                }"
      + "                            ],"
      + "                        },"
      + "                    ],"
      + "                }"
      + "            ],"
      + "        }"
      + "    ]"
      + "}";
}
