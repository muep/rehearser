(ns rehearser.hex)

(defn hex->bytes [s]
  (into [] (map (fn [[hn ln]]
                  (Integer/parseInt (str hn ln) 16))
                (partition 2 s))))
