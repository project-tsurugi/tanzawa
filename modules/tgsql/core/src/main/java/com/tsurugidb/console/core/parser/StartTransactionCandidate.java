package com.tsurugidb.console.core.parser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.tsurugidb.console.core.model.Regioned;
import com.tsurugidb.console.core.model.StartTransactionStatement.ExclusiveMode;
import com.tsurugidb.console.core.model.StartTransactionStatement.ReadWriteMode;
import com.tsurugidb.console.core.model.StartTransactionStatement.TransactionMode;
import com.tsurugidb.console.core.model.Value;

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
