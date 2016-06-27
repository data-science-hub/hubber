Hubber
======

Work in progress...


Triple Store
------------

At the moment only Virtuoso is supported. Make sure these priviledges are set:

    GRANT EXECUTE ON DB.DBA.SPARQL_INSERT_DICT_CONTENT TO "SPARQL";
    GRANT EXECUTE ON DB.DBA.SPARQL_DELETE_DICT_CONTENT TO "SPARQL";

Make also sure that the triple store and its SPARQL endpoint are *not*
accessible from the outside. It is recommended to use a separate Virtuoso
instance just for this.