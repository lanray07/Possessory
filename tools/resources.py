"""Generate the English Android source catalogue. Other locales require translation review."""
from pathlib import Path
from xml.sax.saxutils import escape
import re

ROOT = Path(__file__).resolve().parents[1]
STRINGS = {
    'app_language':'App language','use_device_language':'Device language',
    'kitchen':'Kitchen','bedroom':'Bedroom','bathroom':'Bathroom','home_office':'Home Office','garage':'Garage','garden':'Garden','storage':'Storage','other':'Other',
    'electronics':'Electronics','appliances':'Appliances','furniture':'Furniture','jewellery':'Jewellery','clothing':'Clothing','tools':'Tools','art':'Art',
    'condition_new':'New','condition_good':'Good','condition_fair':'Fair','condition_repair':'Needs repair',
    'status_sold':'Sold','status_donated':'Donated','status_lost':'Lost','status_disposed':'Disposed',
    'premium_active':'Premium entitlement verified.',
    'purchase_verification_notice':'Purchases are verified by the Possessory receipt service. Cached signed access works offline for up to 90 days; reconnect to refresh. Existing records remain accessible.',
    'upgrade':'Unlock Premium','purchase_retry':'Purchase verification could not finish. Restore purchases to retry. No new entitlement has been granted.',
    'add_household':'Add household',
    'app_name':'Possessory', 'tagline':'Everything you own. Beautifully organised.',
    'your_home':'Your home', 'overview':'Overview','inventory':'Inventory','rooms':'Rooms','warranties':'Warranties',
    'maintenance':'Maintenance','reports':'Reports','settings':'Settings','voice':'Voice assistant',
    'add_item':'Add item','add_first_item':'Add your first item','add_room':'Add room','room_name':'Room name',
    'private_by_design':'Private by design','local_promise':'On your device. No account required.',
    'everything_in_place':'Everything, in its place.','dashboard_subtitle':'A little clarity for everything you call home.',
    'a_little_order':'A little order. A lot of peace of mind.', 'hero_title':'A home for\neverything you own.',
    'hero_body':'Your belongings, receipts and warranties.\nThoughtfully kept together.',
    'total_items':'Belongings recorded','recorded_value':'Recorded purchase value','missing_receipts':'Missing receipts',
    'recently_added':'Recently added','start_collection':'Your collection starts here',
    'empty_inventory_body':'Add a belonging to keep its details, receipt and warranty together.',
    'needs_attention':'On the horizon','view_all':'View all','no_warranties_due':'No warranties expiring soon.',
    'create_report':'Create an inventory report','item_count':'%1$d items',
    'your_belongings':'Your belongings','inventory_subtitle':'The things that make your house a home.',
    'search_hint':'Search belongings, brands, serial numbers and notes',
    'all_rooms':'All rooms','all_categories':'All categories','category':'Category','filter':'Filter','sort':'Sort',
    'active':'Active','archived':'Archived','expiring_soon':'Expiring soon','maintenance_due':'Maintenance due',
    'newest':'Recently added','name':'Name','room':'Room','no_items':'Nothing here yet',
    'no_items_body':'Add an item or change your search and filters.',
    'room_by_room':'Room by room.','rooms_subtitle':'Every space, beautifully organised.',
    'room_required':'Create a household and a room before adding belongings.',
    'living_room':'Living Room','warranty_title':'A little peace of mind.',
    'warranty_subtitle':'Keep your warranty details close at hand.','covered':'Within recorded warranty dates',
    'expired':'Expired','nothing_here':'No records in this section.',
    'warranty_disclaimer':'Recorded warranty dates are supplied by you. Manufacturer warranties and statutory consumer rights are separate; eligibility depends on the applicable terms.',
    'save':'Save','cancel':'Cancel','close':'Close','back':'Back','welcome':'Welcome to Possessory',
    'household_name':'Household name','currency':'Currency','create_home':'Create my home','skip':'Skip for now',
    'onboarding_privacy':'Core features work offline. Photos and documents stay in private app storage. Permissions are requested only when you use a feature.',
    'item_name':'Item name','purchase_price':'Purchase price','purchase_date':'Purchase date',
    'warranty_end':'Warranty expiry','warranty_start':'Warranty start','description':'Description','brand':'Brand','model':'Model',
    'serial':'Serial number','retailer':'Retailer','estimated_value':'Your estimated current value',
    'estimate_disclaimer':'Your estimate is separate from the purchase price and is not a professional valuation.',
    'warranty_provider':'Warranty provider','warranty_type':'Warranty type','warranty_notes':'Warranty notes',
    'reminder_days':'Remind before expiry (days)','tags':'Tags','notes':'Notes','condition':'Condition','status':'Status',
    'edit_item':'Edit item','more_details':'More details','fewer_details':'Fewer details',
    'decimal_hint':'Enter a plain amount using a decimal point, without a currency symbol.',
    'form_error':'Check the name, non-negative amounts, and dates (YYYY-MM-DD). Warranty end must follow its start.',
    'item_details':'The details','receipts_documents':'Photos, receipts & documents','add_receipt':'Attach receipt',
    'add_photo':'Add photo','take_photo':'Take photo','photo':'Photo','no_attachments':'No attachments yet.',
    'scan_receipt':'Review and extract receipt','remove_attachment':'Remove attachment','duplicate':'Duplicate',
    'archive':'Archive','unarchive':'Make active','delete':'Delete','delete_item':'Delete this belonging?',
    'delete_item_body':'Its record, photos, receipts and maintenance history will be permanently removed from this device.',
    'review_receipt':'Review receipt','receipt_preview':'Receipt preview',
    'ocr_notice':'English text extraction runs on this device. Review every suggestion. For PDFs, the preview and extraction use the first page; the complete PDF is retained.',
    'rotate':'Rotate','crop_edges':'Trim edges','extract_text':'Extract text','extracted_text':'Extracted text',
    'ocr_unavailable':'Extraction could not complete. You can still enter details manually and keep the attachment.',
    'apply_details':'Apply reviewed details','maintenance_title':'Care for what you keep.',
    'maintenance_subtitle':'A useful rhythm for household upkeep.','add_maintenance':'Add maintenance',
    'no_maintenance':'A fresh start for upkeep','maintenance_empty':'Add a service, filter change or another maintenance task.',
    'maintenance_disclaimer':'Reminders help you organise upkeep and do not replace professional safety inspections.',
    'maintenance_type':'Maintenance type','next_due':'Next scheduled date','repeat_days':'Repeat interval in days (0 = once)',
    'cost':'Recorded maintenance cost','mark_complete':'Mark completed','remove':'Remove',
    'reports_title':'Your home, on record.','reports_subtitle':'A clear inventory, ready when you need it.',
    'build_report':'Prepare your report','all_items':'All active belongings','with_receipts':'With receipts',
    'include_serials':'Include serial numbers','include_prices':'Include recorded values','format':'Format',
    'export_report':'Save report','export_complete':'Your report has been saved.','report_preview':'Report preview',
    'inventory_report':'Household inventory record','report_disclaimer':'User-maintained inventory; not an insurer-certified valuation.',
    'export_privacy':'Check the preview and redaction settings before saving or sharing. Exports may contain personal information. Reports omit personal notes, tags and descriptions.',
    'settings_subtitle':'Make yourself at home.','preferences':'Preferences','dark_theme':'Dark appearance','reminders':'Optional reminders',
    'reminder_timing':'Daily reminders are approximate and depend on device background restrictions and notification settings.',
    'language_system':'Preview translations are bundled and work without internet. They are machine-generated drafts awaiting language and layout review. Your own item names and notes are not translated.',
    'backup_restore':'Backup & restore','backup_body':'Save a complete ZIP with records and attachments. Restore validates checksums and merges by record ID; existing records win. Keep a backup before uninstalling.',
    'encrypted_backup':'Password-protected backup','backup_password':'Password (at least 12 characters)',
    'create_backup':'Save backup','restore_backup':'Open backup','backup_complete':'Backup operation completed.',
    'restore_preview':'Review backup before restoring',
    'restore_preview_body':'%1$d households · %2$d belongings · %3$d attachments\n\nExisting record IDs are kept. New records are added. No existing records are deleted. Backup passwords cannot be recovered.',
    'restore':'Restore','premium':'Possessory Premium','premium_body':'More room for the things you love: unlimited belongings, multiple homes and expanded tools.',
    'premium_unavailable':'Purchases are not enabled in this engineering preview. Store setup and receipt verification are required. The current limit is one household and 30 items; your existing records remain accessible.',
    'restore_purchases':'Restore purchases','privacy':'Your privacy','privacy_body':'Your inventory and attachments are stored in this app’s private storage. No advertising or analytics SDKs are included. Nothing is silently synced. Speech recognition may use your device speech provider’s network service. Exports leave app storage only when you choose a destination. Database and attachments are not separately encrypted by this app; password-protected backups use AES-GCM.',
    'delete_all':'Delete all local data','delete_all_body':'Permanently remove every household, item, attachment and setting on this device? Backups you exported are not deleted.',
    'preview_build':'Engineering preview 0.2.0 · Store billing, translation review and physical Fire-device release checks are pending.',
    'voice_title':'A helping voice.','voice_notice':'Push to talk, then review the transcript. Recognition availability depends on your device and may require internet. Raw audio is not stored by Possessory. Commands in this preview use English.',
    'listening':'Listening… tap to stop','tap_to_speak':'Tap to speak','voice_unavailable':'Speech or the command is unavailable. Type one of the supported commands below.',
    'voice_command':'Your command','voice_examples':'Try “Find my laptop”, “Show items in the kitchen”, “Add a new item”, “Show warranties” or “Show my inventory summary”.',
    'review_command':'Open requested screen','voice_confirm':'Opening your requested screen.',
    'operation_failed':'Unable to complete that action','operation_error':'The operation failed. Check the file, available storage, permissions or entered details, then try again. Your existing records have been retained.'
}
def main():
    refs=set()
    for p in (ROOT/'app/src').rglob('*.kt'):
        refs.update(re.findall(r'R\.string\.(\w+)',p.read_text(encoding='utf-8')))
    assert not refs-STRINGS.keys(), f'Missing strings: {refs-STRINGS.keys()}'
    path=ROOT/'app/src/main/res/values/strings.xml'
    xml=['<?xml version="1.0" encoding="utf-8"?>','<resources>']
    for key,value in STRINGS.items():
        value=escape(value).replace("'", "\\'").replace('\n','\\n')
        xml.append(f'    <string name="{key}">{value}</string>')
    xml.append('    <plurals name="reminder_count"><item quantity="one">%d household reminder needs attention.</item><item quantity="other">%d household reminders need attention.</item></plurals>')
    xml.append('    <plurals name="inventory_count"><item quantity="one">%d item</item><item quantity="other">%d items</item></plurals>')
    xml.append('</resources>')
    path.write_text('\n'.join(xml)+'\n',encoding='utf-8')
    print(f'{len(STRINGS)} source strings generated')
if __name__=='__main__':main()
