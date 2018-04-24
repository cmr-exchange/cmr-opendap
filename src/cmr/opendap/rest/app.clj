(ns cmr.opendap.rest.app
  (:require
   [clojure.java.io :as io]
   [cmr.opendap.rest.handler.core :as handler]
   [cmr.opendap.rest.middleware :as middleware]
   [cmr.opendap.rest.route :as route]
   [ring.middleware.defaults :as ring-defaults]
   [reitit.ring :as ring]
   [taoensso.timbre :as log]))

(defn rest-api-routes
  [httpd-component]
  (concat
   (route/main httpd-component)
   (route/ous-api httpd-component)
   (route/redirects httpd-component)
   (route/static httpd-component)
   (route/admin httpd-component)
   route/testing))

(defn app
  [httpd-component]
  (-> httpd-component
      rest-api-routes
      (ring/router (middleware/reitit-auth httpd-component))
      ;(ring/ring-handler handler/fallback)
      ring/ring-handler
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)
      (middleware/wrap-resource httpd-component)
      middleware/wrap-trailing-slash
      middleware/wrap-cors
      (middleware/wrap-not-found)
      ;middleware/wrap-debug
      ))
