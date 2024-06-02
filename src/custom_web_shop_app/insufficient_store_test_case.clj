(ns custom-web-shop-app.insufficient-store-test-case)


; Products
(def products
  ["Apple"]) ;;single product

; Stores
(def stores
  ["Aldi"])

; The price of each item in each store.
;
; An apple costs €0.25 in Aldi, etc.
(def prices
  ; Aldi
  [[0.25]]) ;; APple

; The number of items left of each item in each store.
;
; One store with 5 apples left, 5 avocados left, etc.
(def stock
  ; Aldi
  [[5] ; only 5 Apple in stock
   ])

; Only 5 customers should be able to buy the product and
; rest of customers should be sent back to their homes
;
; E.g. all customers wants to buy 1 apple
(def customers
  [
   {:id 0 :products [["Apple" 1]]}
   {:id 1 :products [["Apple" 1]]}
   {:id 2 :products [["Apple" 1]]}
   {:id 3 :products [["Apple" 1]]}
   {:id 4 :products [["Apple" 1]]}
   {:id 5 :products [["Apple" 1]]}
   {:id 6 :products [["Apple" 1]]} 
   {:id 7 :products [["Apple" 1]]} 
   {:id 8 :products [["Apple" 1]]} 
   {:id 9 :products [["Apple" 1]]} 
  ]
  )

; Time in milliseconds between sales periods
(def TIME_BETWEEN_SALES 50)
; Time in milliseconds of sales period
(def TIME_OF_SALES 10)
