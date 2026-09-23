(ns outdated-deps.core
  "Print dependencies in a deps.edn file that have a newer release.

  Each dependency listed with :mvn/version in the file's :deps and
  alias :extra-deps / :replace-deps is compared against the versions
  published in Maven Central and Clojars. Pre-release versions are
  ignored when picking the latest."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]))

(def ^:private repos
  ["https://repo1.maven.org/maven2/"
   "https://repo.clojars.org/"])

(def ^:private pre-release-pattern
  #"(?i)(alpha|beta|rc|snapshot|milestone|preview|-m\d)")

(defn- http-get [url]
  (let [conn (doto (.openConnection (java.net.URL. url))
               (.setConnectTimeout 15000)
               (.setReadTimeout 15000)
               (.setRequestProperty "User-Agent" "rehearser-outdated-deps-check"))]
    (with-open [is (.getInputStream conn)]
      (slurp is))))

(defn- metadata-urls [coordinate]
  (let [[group artifact] (str/split coordinate #"/")
        group-path (str/replace group "." "/")]
    (map #(str % group-path "/" artifact "/maven-metadata.xml") repos)))

(defn- fetch-versions [coordinate]
  (->> (metadata-urls coordinate)
       (keep (fn [url]
               (try (->> (http-get url)
                         (re-seq #"<version>([^<]+)</version>")
                         (map second))
                 (catch Exception _ nil))))
       (apply concat)
       distinct))

(defn- version-key [version]
  (mapv #(Long/parseLong %) (str/split version #"\.")))

(defn- compare-versions [a b]
  (let [pad (fn [key] (concat key (repeat (- (max (count a) (count b)) (count key)) 0)))]
    (compare (vec (pad a)) (vec (pad b)))))

(defn- latest-stable [versions]
  (->> versions
       (remove #(re-find pre-release-pattern %))
       (filter #(re-matches #"\d+(\.\d+)*" %))
       (sort-by version-key compare-versions)
       last))

(defn- collect-coordinates [form]
  (cond
    (map? form)
    (if (every? map? (vals form))
      (keep (fn [[coordinate version-map]]
              (when (and (symbol? coordinate) (:mvn/version version-map))
                [coordinate (:mvn/version version-map)]))
            form)
      (mapcat collect-coordinates (vals form)))
    (sequential? form) (mapcat collect-coordinates form)
    :else ()))

(defn- dependencies [deps-edn]
  (let [alias-deps (mapcat #(cond->> % (map? %) vals) (vals (:aliases deps-edn)))]
    (distinct (map (fn [[coordinate version]] [(str coordinate) version])
                   (mapcat collect-coordinates (cons (:deps deps-edn) alias-deps))))))

(defn -main [& args]
  (let [deps-file (or (first args) "deps.edn")
        deps-edn (edn/read-string (slurp deps-file))]
    (doseq [[coordinate current] (sort (dependencies deps-edn))]
      (let [versions (fetch-versions coordinate)
            latest (latest-stable versions)]
        (cond
          (empty? versions)
          (println coordinate current "NOT-FOUND")

          (not= latest current)
          (println coordinate current "->" latest)

          :else
          (println coordinate current "OK"))))
    (shutdown-agents)))
