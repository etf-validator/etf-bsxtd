(:~
 :
 : -------------------------------------------------
 : Object Preparation XQuery Function Library Facade
 : -------------------------------------------------
 :
 : Copyright (C) 2019 interactive instruments GmbH
 :
 : Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 : the European Commission - subsequent versions of the EUPL (the "Licence");
 : You may not use this work except in compliance with the Licence.
 : You may obtain a copy of the Licence at:
 :
 : https://joinup.ec.europa.eu/software/page/eupl
 :
 : Unless required by applicable law or agreed to in writing, software
 : distributed under the Licence is distributed on an "AS IS" basis,
 : WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 : See the Licence for the specific language governing permissions and
 : limitations under the Licence.
 :
 : @author  Jon Herrmann ( herrmann aT interactive-instruments doT de )
 :)
module namespace isolate = 'https://modules.etf-validator.net/isolate-data/1';

declare namespace db = 'http://basex.org/modules/db';
declare copy-namespaces preserve, inherit;

(: set to perform read operations :)
declare %basex:lazy variable $isolate:dbNames external := '';
declare %basex:lazy variable $isolate:dbRead := try { for $db in tokenize($isolate:dbNames, ',') return db:open($db) } catch * {
    error(xs:QName("isolate:Error"), concat('Databases ', $isolate:dbNames, ' could not be opened for reading. Error: ', $err:description))
};

(: set to perform write operations :)
declare %basex:lazy variable $isolate:dbName external := '';
declare %basex:lazy variable $isolate:dbWrite := try { db:open($isolate:dbName) } catch * {
    error(xs:QName("isolate:Error"), concat('Database ', $isolate:dbName, ' could not be opened for writing. Error: ', $err:description))
};

(:~
 : Insert a node into the $dbName database and remove it from its original database
 :)
declare updating function isolate:erroneousObject($object as node(), $error as node()) as empty-sequence() {
    insert node <isolate:ErroneousObject>{$error} {$object}</isolate:ErroneousObject>
    into $isolate:dbWrite,
    delete node $object
};

(:~
 : Remove an erroneous object from the database and save an error message
 :)
declare updating function isolate:pruneErroneousObject($object as node(), $error as node()) as empty-sequence() {
    insert node <isolate:ErroneousObject>{$error}</isolate:ErroneousObject>
    into $isolate:dbWrite,
    delete node $object
};

(:~
 : Get a sequence of objects and their errors from the database.
 : The error node is stored in position 1, the object at position 2
 :)
declare function isolate:erroneousObjects() as node()* {
    $isolate:dbRead/isolate:ErroneousObject
};

(:~
 : Get only the erroneous objects without the errors
 :)
declare function isolate:plainErroneousObjects() as node()* {
    isolate:erroneousObjects()/*[2]
};
