/*
 * Copyright 2023-2024 Project Tsurugi.
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
 */
package com.tsurugidb.tgsql.core.parser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.tsurugidb.tgsql.core.model.Regioned;
import com.tsurugidb.tgsql.core.model.Value;
import com.tsurugidb.tgsql.core.model.StartTransactionStatement.ExclusiveMode;
import com.tsurugidb.tgsql.core.model.StartTransactionStatement.ReadWriteMode;
import com.tsurugidb.tgsql.core.model.StartTransactionStatement.TransactionMode;

class StartTransactionCandidate {

    Regioned<TransactionMode> transactionMode;

    Regioned<ReadWriteMode> readWriteMode;

    Regioned<ExclusiveMode> exclusiveMode;

    List<Regioned<String>> writePreserve;

    Regioned<Boolean> includeDdl;

    List<Regioned<String>> readAreaInclude;

    List<Regioned<String>> readAreaExclude;

    Regioned<String> label;

    Map<Regioned<String>, Optional<Regioned<Value>>> properties;
}
