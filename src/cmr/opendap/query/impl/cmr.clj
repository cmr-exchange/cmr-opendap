(ns cmr.opendap.query.impl.cmr
  (:require
   [cmr.opendap.query.const :as const]
   [cmr.opendap.ous.util.core :as ous-util]
   [cmr.opendap.util :as util]
   [taoensso.timbre :as log]))

(def collection-behaviour
  {:->cmr identity})

(defn not-array?
  [array]
  (or (nil? array)
      (empty? array)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Implementation of Collection Params API   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord CollectionCmrStyleParams
  [;; `collection-id` is the concept id for the collection in question. Note
   ;; that the collection concept id is not provided in query params,
   ;; but in the path as part of the REST URL. Regardless, we offer it here as
   ;; a record field.
   collection-id
   ;;
   ;; `format` is any of the formats supported by the target OPeNDAP server,
   ;; such as `json`, `ascii`, `nc`, `nc4`, `dods`, etc.
   format
   ;;
   ;; `granules` is list of granule concept ids; default behaviour is a
   ;; whitelist.
   granules
   ;;
   ;; `exclude-granules` is a boolean when set to true causes granules list
   ;; to be a blacklist.
   exclude-granules
   ;;
   ;; `variables` is a list of variables to be speficied when creating the
   ;; OPeNDAP URL. This is used for subsetting.
   variables
   ;;
   ;; `subset` is used the same way as `subset` for WCS where latitudes,
   ;; lower then upper, are given together and then longitude (again, lower
   ;; then upper) are given together. For instance, to indicate desired
   ;; spatial subsetting in URL queries:
   ;;  `?subset=lat(56.109375,67.640625)&subset=lon(-9.984375,19.828125)`
   subset
   ;;
   ;; `bounding-box` is provided for CMR/EDSC-compatibility as an alternative
   ;; to using `subset` for spatial-subsetting. This parameter describes a
   ;; rectangular area of interest using four comma-separated values:
   ;;  1. lower left longitude
   ;;  2. lower left latitude
   ;;  3. upper right longitude
   ;;  4. upper right latitude
   ;; For example:
   ;;  `bounding_box==-9.984375,56.109375,19.828125,67.640625`
   bounding-box
   ;; `temporal` is used to indicate temporal subsetting with starting
   ;; and ending values being ISO 8601 datetime stamps.
   temporal])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Constructor   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create
  [params]
  (log/trace "Instantiating params protocol ...")
  (let [bounding-box (ous-util/split-comma->coll (:bounding-box params))
        subset (:subset params)
        granules-array (ous-util/split-comma->coll
                        (get params (keyword "granules[]")))
        variables-array (ous-util/split-comma->coll
                         (get params (keyword "variables[]")))
        temporal-array (ous-util/->coll
                        (get params (keyword "temporal[]")))]
    (log/trace "original bounding-box:" (:bounding-box params))
    (log/trace "bounding-box:" bounding-box)
    (log/trace "subset:" subset)
    (log/trace "granules-array:" granules-array)
    (log/trace "variables-array:" variables-array)
    (map->CollectionCmrStyleParams
      (assoc params
        :format (or (:format params) const/default-format)
        :granules (if (not-array? granules-array)
                    (ous-util/split-comma->sorted-coll (:granules params))
                    granules-array)
        :variables (if (not-array? variables-array)
                     (ous-util/split-comma->sorted-coll (:variables params))
                     variables-array)
        :exclude-granules (util/bool (:exclude-granules params))
        :subset (if (seq bounding-box)
                 (ous-util/bounding-box->subset bounding-box)
                 (:subset params))
        :bounding-box (if (seq bounding-box)
                        (mapv #(Float/parseFloat %) bounding-box)
                        (when (seq subset)
                          (ous-util/subset->bounding-box subset)))
        :temporal (if (not-array? temporal-array)
                    (ous-util/->coll (:temporal params))
                    temporal-array)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Implementation of Collections Params API   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord CollectionsCmrStyleParams
  [;; This isn't defined for the OUS Prototype, since it didn't support
   ;; submitting multiple collections at a time. As such, there is no
   ;; prototype-oriented record for this.
   ;;
   ;; `collections` is a list of `CollectionCmrStyleParams` records.
   collections])

(def collections-behaviour
  ;; Reserved for later use.
  {})
