(ns rehearser.misc)

(defn select-or-nil-keys [m keyseq]
  (merge
   (into {} (map (fn [k] [k nil]) keyseq))
   (select-keys m keyseq)))
