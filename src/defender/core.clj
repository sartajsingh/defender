(ns defender.core
  (:require [com.netflix.hystrix.core :as hystrix]
            [clojure.tools.logging :as log])
  (:import (com.netflix.hystrix HystrixCommand HystrixCommand$Setter
                                HystrixCommandProperties HystrixThreadPoolProperties
                                HystrixExecutable)
           (com.netflix hystrix.strategy HystrixPlugins)
           (com.netflix.hystrix.contrib.codahalemetricspublisher HystrixCodaHaleMetricsPublisher)
           (com.codahale.metrics MetricRegistry)))

(defn init-fn [{:keys [thread-count
                       breaker-sleep-window-ms
                       breaker-error-threshold-percentage
                       execution-timeout-ms]}]
  (fn [_ ^HystrixCommand$Setter setter]
    (-> setter
        (.andCommandPropertiesDefaults
          (-> (HystrixCommandProperties/Setter)
              (.withCircuitBreakerSleepWindowInMilliseconds breaker-sleep-window-ms)
              (.withCircuitBreakerErrorThresholdPercentage breaker-error-threshold-percentage)
              (.withExecutionTimeoutInMilliseconds execution-timeout-ms)))
        (.andThreadPoolPropertiesDefaults
          (-> (HystrixThreadPoolProperties/Setter)
              (.withCoreSize thread-count))))))

(defn- fallback-wrapper [fallback-fn]
  (capture-logging-context
    (fn []
      (let [^HystrixCommand command hystrix/*command*]
        (fallback-fn {:response-timed-out?         (.isResponseTimedOut command)
                      :failed-execution-exeception (.getFailedExecutionException command)
                      :circuit-breaker-open?       (.isCircuitBreakerOpen command)})))))

(defn hystrix-conf [{:keys [config group-key thread-pool-key command-key run-fn fallback-fn]}]
  (cond-> {:type            :command
           :group-key       (hystrix/group-key group-key)
           :command-key     (hystrix/command-key command-key)
           :thread-pool-key (hystrix/thread-pool-key (or thread-pool-key group-key))
           :init-fn         (init-fn config)
           :run-fn          (capture-logging-context run-fn)}

    (some? fallback-fn) (assoc :fallback-fn (fallback-wrapper fallback-fn))))

(defn execute-with-hystrix [key-map]
  (let [definition (hystrix-conf key-map)]
    (.execute ^HystrixExecutable (hystrix/instantiate* definition))))

(defmacro with-hystrix [key-map & body]
  `(execute-with-hystrix (assoc ~key-map :run-fn (fn [] ~@body))))

(defn register-metrics-publisher [^MetricRegistry metrics-registry]
  (.registerMetricsPublisher (HystrixPlugins/getInstance) (HystrixCodaHaleMetricsPublisher. metrics-registry))
  (log/info "Registered hystrix metrics publisher"))
