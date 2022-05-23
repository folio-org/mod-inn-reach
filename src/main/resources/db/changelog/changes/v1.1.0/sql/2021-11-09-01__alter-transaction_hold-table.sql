ALTER TABLE transaction_hold
  ADD COLUMN folio_instance_id UUID,
  ADD COLUMN folio_holding_id UUID,
  ADD COLUMN folio_patron_barcode VARCHAR(255),
  ADD COLUMN folio_item_barcode VARCHAR(255);
