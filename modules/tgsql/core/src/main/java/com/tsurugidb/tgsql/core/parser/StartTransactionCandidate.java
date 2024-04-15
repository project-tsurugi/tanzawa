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
