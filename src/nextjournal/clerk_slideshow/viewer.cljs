(ns nextjournal.clerk-slideshow.viewer
  (:require [nextjournal.clerk.sci-viewer :as v]
            [reagent.core :as reagent]))

(defonce !state (reagent/atom {:current-slide 0
                               :grid? false
                               :viewport-width js/innerWidth
                               :viewport-height js/innerHeight}))

(defn render-slides [slides]
  (v/html
   (reagent/with-let [ref-fn (fn [el]
                               (when el
                                 (swap! !state assoc :stage-el el)
                                 (js/addEventListener "resize"
                                                      #(swap! !state assoc
                                                              :viewport-width js/innerWidth
                                                              :viewport-height js/innerHeight))
                                 (js/document.addEventListener "keydown"
                                                               (fn [e]
                                                                 (case (.-key e)
                                                                   "Escape" (swap! !state update :grid? not)
                                                                   "ArrowRight" (when-not (:grid? !state)
                                                                                  (swap! !state update :current-slide #(min (dec (count slides)) (inc %))))
                                                                   "ArrowLeft" (when-not (:grid? !state)
                                                                                 (swap! !state update :current-slide #(max 0 (dec %))))
                                                                   nil)))))
                      default-transition {:type :spring :duration 0.4 :bounce 0.1}]
     (let [{:keys [grid? current-slide viewport-width viewport-height]} @!state]
       [:div.overflow-hidden.relative.bg-slate-50
        {:ref ref-fn :id "stage" :style {:width viewport-width :height viewport-height}}
        (into [:> (.. framer-motion -motion -div)
               {:style {:width (if grid? viewport-width (* (count slides) viewport-width))}
                :initial false
                :animate {:x (if grid? 0 (* -1 current-slide viewport-width))}
                :transition default-transition}]
              (map-indexed
               (fn [i slide]
                 (let [width 250
                       height 150
                       gap 40
                       slides-per-row (int (/ viewport-width (+ gap width)))
                       col (mod i slides-per-row)
                       row (int (/ i slides-per-row))]
                   [:> (.. framer-motion -motion -div)
                    {:initial false
                     :class ["absolute left-0 top-0 overflow-x-hidden bg-white"
                             (when grid?
                               "rounded-lg shadow-lg overflow-y-hidden cursor-pointer ring-1 ring-slate-200 hover:ring hover:ring-blue-500/50 active:ring-blue-500")]
                     :animate {:width (if grid? width viewport-width)
                               :height (if grid? height viewport-height)
                               :x (if grid? (+ gap (* (+ gap width) col)) (* i viewport-width))
                               :y (if grid? (+ gap (* (+ gap height) row)) 0)}
                     :transition default-transition
                     :on-click #(when grid? (swap! !state assoc :current-slide i :grid? false))}
                    [:> (.. framer-motion -motion -div)
                     {:style {:width viewport-width
                              :height viewport-height
                              :transformOrigin "left top"}
                      :initial false
                      :animate {:scale (if grid? (/ width viewport-width) 1)}
                      :transition default-transition}
                     slide]]))
               slides))]))))
