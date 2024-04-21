(ns logseq.outliner.transaction
  "Provides a wrapper around logseq.outliner.datascript/transact! using
   transient state from logseq.outliner.core"
  #?(:cljs (:require-macros [logseq.outliner.transaction]))
  #?(:cljs (:require [malli.core :as m])))

#_:clj-kondo/ignore
(def ^:private transact-opts [:or :symbol :map])

#?(:org.babashka/nbb nil
   :cljs (m/=> transact! [:=> [:cat transact-opts :any] :any]))

(defmacro ^:api transact!
  "Batch all the transactions in `body` to a single transaction, Support nested transact! calls.
  Currently there are no options, it'll execute body and collect all transaction data generated by body.
  If no transactions are included in `body`, it does not save a transaction.
  `Args`:
    `opts`: Every key is optional, opts except `additional-tx` will be transacted as `tx-meta`.
            {:transact-opts {:conn conn} \"Datascript conn\"
             :additional-tx \"Additional tx data that can be bundled together
                              with the body in this macro.\"
             :persist-op? \"Boolean, store ops into db (sqlite), by default,
                            its value depends on (config/db-based-graph? repo)\"}
  `Example`:
  (transact! {:conn db-conn}
    (insert-blocks! ...)
    ;; do something
    (move-blocks! ...)
    (delete-blocks! ...))"
  [opts & body]
  `(let [opts# (dissoc ~opts :transact-opts :current-block)]
     (frontend.worker.batch-tx/with-batch-tx-mode
      (:conn (:transact-opts ~opts))
      opts#
      ~@body)))
