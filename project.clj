(defproject defender "0.1.0-SNAPSHOT"
  :description "The Defender"
  :url "https://github.com/sartajsingh/defender.git"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.netflix.hystrix/hystrix-clj "1.5.13"]
                 [com.netflix.hystrix/hystrix-codahale-metrics-publisher "1.5.13"
                  :exclusions [io.dropwizard.metrics/metrics-core]]
                 [com.netflix.hystrix/hystrix-metrics-event-stream "1.5.13"]])
