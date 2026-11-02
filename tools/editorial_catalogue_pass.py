"""Explicit semantic corrections from the remaining-catalogue review; no automatic approval."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def entries(text):
    return dict(line.split('|', 1) for line in text.strip().splitlines())

ES = entries(r'''
clothing|Ropa
condition_good|Buen estado
status_lost|Perdido
tagline|Todo lo que tienes. Bien organizado.
a_little_order|Un poco de orden. Mucha tranquilidad.
start_collection|Tu colección empieza aquí
empty_inventory_body|Añade una pertenencia para guardar juntos sus datos, recibo y garantía.
item_count|%1$d artículos
your_belongings|Tus pertenencias
expiring_soon|Próximo a vencer
room_required|Crea un hogar y una habitación antes de añadir pertenencias.
warranty_title|Un poco de tranquilidad.
warranty_disclaimer|Tú proporcionas las fechas de garantía registradas. Las garantías del fabricante y los derechos legales del consumidor son independientes; la cobertura depende de las condiciones aplicables.
onboarding_privacy|Las funciones principales funcionan sin conexión. Las fotos y los documentos permanecen en el almacenamiento privado de la aplicación. Los permisos solo se solicitan al utilizar una función.
edit_item|Editar artículo
decimal_hint|Introduce el importe con el separador decimal de la configuración regional seleccionada, sin símbolo de moneda ni separadores de miles.
receipts_documents|Fotos, recibos y documentos
add_receipt|Adjuntar recibo
take_photo|Hacer una foto
no_attachments|Aún no hay archivos adjuntos.
scan_receipt|Revisar y extraer datos del recibo
delete|Eliminar
delete_item|¿Eliminar esta pertenencia?
review_receipt|Revisar recibo
receipt_preview|Vista previa del recibo
ocr_notice|El texto del recibo se extrae en este dispositivo en el idioma seleccionado. Elige una página del PDF, gira o recorta la imagen de revisión y comprueba cada sugerencia. Los archivos adjuntos originales se conservan.
rotate|Girar
crop_edges|Recortar bordes
extract_text|Extraer texto
ocr_unavailable|No se pudo completar la extracción. Puedes introducir los datos manualmente y conservar el archivo adjunto.
apply_details|Aplicar datos revisados
export_report|Guardar informe
export_complete|El informe se ha guardado.
report_preview|Vista previa del informe
language_system|Las traducciones de prueba se incluyen en la aplicación y funcionan sin internet. Son borradores generados automáticamente pendientes de revisión del idioma y del diseño. Los nombres y las notas que introduces no se traducen.
restore|Restaurar
premium_unavailable|Las compras no están habilitadas en esta versión de prueba. Es necesario configurar la tienda y la verificación de compras. El límite actual es un hogar y 30 artículos; tus registros existentes siguen accesibles.
preview_build|Possessory 0.3.0 · La aprobación de idiomas, la verificación de compras y las comprobaciones finales de publicación se registran por separado.
voice_unavailable|El reconocimiento de voz o el comando no está disponible. Escribe uno de los comandos admitidos que aparecen abajo.
review_command|Revisar comando
clear_filters|Borrar filtros
unlock_notice|Usa el PIN, patrón o contraseña de bloqueo del dispositivo para abrir Possessory.
app_lock_notice|Requiere un PIN, patrón o contraseña del dispositivo. Possessory no guarda esa credencial. El bloqueo también oculta las capturas de pantalla y las vistas previas de aplicaciones recientes.
add_document|Añadir documento
delete_maintenance_body|Se eliminarán el registro de mantenimiento y sus documentos adjuntos. Los demás registros de artículos se conservarán.
reminder_offsets|Días de antelación de los recordatorios, separados por comas
custom_reminder_hint|La antelación se indica en días antes del vencimiento, por ejemplo 30,14,7,0. Las fechas adicionales usan YYYY-MM-DD. Los ajustes de ahorro de energía del dispositivo pueden retrasar las comprobaciones diarias.
reset_crop|Restablecer imagen de revisión
ocr_page|Página del PDF
ocr_language|Idioma del recibo
high_value|Artículos de gran valor
column_item|Artículo
no_receipt_reference|Sin referencias a archivos adjuntos
voice_count_result|%d pertenencias corresponden a esta habitación.
voice_move_title|Confirmar traslado del artículo
review_mode_notice|VERSIÓN DE EVALUACIÓN: traducciones provisionales y herramientas de evaluación habilitadas. No se concede ninguna compra ni derecho a Premium. No publicar en Appstore.
store:title|Possessory – Inventario del hogar y garantías
store:iap_title|Possessory Premium
store:iap_description|Mejora mediante un pago único para disponer de pertenencias y hogares ilimitados, OCR de recibos, recordatorios avanzados de garantía, mantenimiento, informes PDF/CSV/JSON, búsquedas guardadas, comandos de voz ampliados y copias de seguridad con contraseña. Sin suscripción. Verificar la compra requiere internet.
support:title|Ayuda de Possessory
support:contact_label|Contactar con soporte
support:draft_notice|Borrador de ayuda para la versión de prueba. La validación por dispositivo y la revisión de traducciones están pendientes.
''')
ES.update({
 'support:premium':'Premium es una compra única de Amazon, sin suscripción. La tienda muestra el precio local. Comprar y restaurar requieren internet. Los identificadores de usuario de Amazon y del recibo de compra se envían por HTTPS al verificador de Possessory alojado en Cloudflare Workers, que consulta a Amazon y devuelve un derecho de acceso firmado. No se envían el inventario del hogar ni las fotos de recibos. El verificador utiliza la dirección IP de la conexión y hashes de los identificadores de compra para limitar temporalmente la frecuencia de solicitudes. No tiene una base de datos de clientes y el registro de solicitudes de la aplicación está desactivado. Cloudflare procesa metadatos de red para operar y proteger su servicio. El acceso firmado sin conexión dura hasta 90 días, o hasta que surta efecto una cancelación verificada anterior, antes de necesitar renovación. Los registros existentes, la edición normal, la eliminación y la restauración de copias de seguridad siguen disponibles sin Premium.',
 'store:description':'Un lugar para todo lo que tienes. Organiza tus pertenencias por habitación con fotos, recibos, datos de compra y fechas de garantía. Busca registros, controla garantías y guarda copias de seguridad ZIP en tu dispositivo. El inventario básico funciona sin conexión y no requiere cuenta. La versión gratuita incluye un hogar, hasta 30 pertenencias y tres archivos adjuntos por pertenencia. Premium, una compra única opcional, añade pertenencias y hogares ilimitados, OCR de recibos, recordatorios avanzados, mantenimiento, exportaciones PDF/CSV/JSON, búsquedas guardadas, comandos de voz ampliados y copias de seguridad con contraseña. No tiene suscripción; Amazon muestra el precio local. Verificar la compra requiere internet; el acceso firmado sin conexión dura hasta 90 días antes de renovarse. El reconocimiento de voz depende del proveedor del dispositivo y puede usar un servicio de red. Revisa las sugerencias del recibo antes de aplicarlas. Los informes son registros mantenidos por el usuario, no valoraciones certificadas por una aseguradora.',
 'support:getting_started':'Crea un hogar, elige su moneda y añade habitaciones. Añade una pertenencia con sus datos de compra y, si lo deseas, la fecha de vencimiento de la garantía. No necesitas una cuenta.',
 'support:where_is_my_data':'Los registros del inventario y los archivos adjuntos se guardan en el almacenamiento privado de la aplicación en este dispositivo. Nada se sincroniza sin avisarte. Desinstalar la aplicación elimina los datos locales; guarda y verifica antes una copia de seguridad.',
 'support:receipts':'Adjunta una imagen o un PDF del recibo, elige la página y el idioma, y recorta o gira la imagen antes de extraer el texto en el dispositivo. Comprueba cada sugerencia antes de aplicarla. Se conserva el archivo original completo. Nunca se deducen cambios de moneda sin avisarte.',
 'support:backup':'En Ajustes, guarda una copia ZIP con los metadatos y los archivos adjuntos. Puedes protegerla con una contraseña de al menos doce caracteres. Guárdala de forma segura: no se puede recuperar. La restauración admite hasta 100 MiB descomprimidos; cada archivo adjunto importado tiene un límite de 25 MiB. Los metadatos de la copia tienen un límite de 8 MiB. Durante la restauración, los adjuntos se guardan temporalmente en la caché privada para limitar el uso de memoria.',
 'support:restore':'Abre una copia de seguridad en Ajustes, introduce su contraseña si está cifrada y revisa la vista previa de importación. Possessory comprueba la estructura de datos, los hashes de los archivos adjuntos y las referencias de los registros. Conserva los identificadores de registros existentes y solo incorpora registros nuevos. Verifica tus pertenencias y adjuntos después de restaurar.',
 'support:reminders':'Activa los recordatorios opcionales en Ajustes y permite las notificaciones cuando se solicite. Possessory comprueba aproximadamente cada día las próximas fechas de garantía y el mantenimiento programado. La gestión de energía del dispositivo puede retrasar las notificaciones. Los recordatorios no sustituyen las inspecciones de seguridad profesionales.',
 'support:voice':'Usa las frases de comando que muestra la aplicación y revisa la transcripción. Las acciones admitidas incluyen navegar, buscar pertenencias, contar objetos por habitación, crear un borrador de artículo, leer recordatorios de garantía y trasladar un artículo identificado de forma única después de confirmarlo. Las acciones ampliadas requieren Premium. Si no hay reconocimiento de voz, escribe el comando; el proveedor de voz puede utilizar un servicio de red.',
 'support:reports':'Elige una habitación, categoría o filtro de valor para una moneda concreta y un formato de salida. Revisa los ajustes de números de serie, valores registrados, fotos y referencias de documentos antes de guardar. Consulta el resumen paginado o abre la vista previa del PDF. Los informes son registros del inventario mantenidos por el usuario y no valoraciones certificadas por una aseguradora.',
 'support:warranties':'Tú proporcionas las fechas y condiciones de garantía. Las garantías del fabricante y los derechos legales del consumidor son independientes. Possessory no determina si tienes derecho a cobertura ni proporciona asesoramiento jurídico definitivo.',
 'support:delete':'Elimina una pertenencia desde su pantalla de detalles para borrar su registro, adjuntos y entradas de mantenimiento. Para eliminar todos los registros y ajustes locales, selecciona Eliminar todos los datos locales en Ajustes y confirma. Los archivos que hayas exportado a otro lugar no se eliminan.',
 'support:app_lock':'Configura un PIN, patrón o contraseña del dispositivo y activa la protección de desbloqueo en Ajustes. Possessory usa la pantalla de credenciales del sistema y no guarda esa credencial. El bloqueo oculta las vistas previas de aplicaciones recientes y las capturas de pantalla. Esta protección es independiente del cifrado de datos almacenados.',
})

FR = entries(r'''
premium|Possessory Premium
store:iap_title|Possessory Premium
store:title|Possessory – Inventaire du foyer et garanties
support:title|Assistance Possessory
support:contact_label|Contacter l’assistance
support:draft_notice|Brouillon d’aide pour la version d’évaluation. La validation sur les appareils et la révision des traductions restent à effectuer.
backup_complete|Opération de sauvegarde terminée.
restore|Restaurer
premium_unavailable|Les achats ne sont pas activés dans cette version d’évaluation. La boutique et la vérification des achats doivent être configurées. La limite actuelle est d’un foyer et de 30 objets ; vos enregistrements existants restent accessibles.
preview_build|Possessory 0.3.0 · L’approbation des langues, la vérification des achats et les contrôles finaux de publication sont suivis séparément.
listening|Écoute en cours… appuyez pour arrêter
tap_to_speak|Appuyez pour parler
voice_unavailable|La reconnaissance vocale ou la commande n’est pas disponible. Saisissez l’une des commandes prises en charge ci-dessous.
review_command|Vérifier la commande
household_currency_notice|La devise du foyer sert aux nouveaux enregistrements. Les devises et les valeurs des objets existants sont conservées.
unlock_notice|Utilisez le code PIN, le schéma ou le mot de passe de verrouillage de votre appareil pour ouvrir Possessory.
unlock|Déverrouiller
app_lock_notice|Nécessite un code PIN, un schéma ou un mot de passe de l’appareil. Possessory ne conserve pas cet identifiant de déverrouillage. Le verrouillage masque aussi les captures d’écran et les aperçus des applications récentes.
custom_reminder_hint|Les délais correspondent au nombre de jours avant l’expiration, par exemple 30,14,7,0. Les dates supplémentaires utilisent YYYY-MM-DD. Les réglages d’économie d’énergie de l’appareil peuvent retarder les contrôles quotidiens.
reset_crop|Réinitialiser l’image à vérifier
ocr_page|Page du PDF
ocr_language|Langue du reçu
column_item|Objet
column_warranty|Expiration de la garantie
voice_count_result|%d objets correspondent à cette pièce.
voice_room_missing|Aucune pièce correspondante trouvée. Sélectionnez une pièce à l’écran.
review_mode_notice|VERSION D’ÉVALUATION : traductions provisoires et outils d’évaluation activés. Aucun achat ni droit à Premium n’est accordé. Ne pas publier sur Appstore.
language_system|Les traductions de la version d’évaluation sont intégrées et fonctionnent sans internet. Ces brouillons générés automatiquement attendent une révision linguistique et visuelle. Les noms d’objets et les notes que vous saisissez ne sont pas traduits.
''')
FR.update({
 'store:description':'Une place pour tout ce que vous possédez. Organisez les objets du foyer par pièce avec photos, reçus, détails d’achat et dates de garantie. Recherchez vos enregistrements, suivez les garanties et enregistrez des sauvegardes ZIP sur votre appareil. L’inventaire de base fonctionne hors ligne, sans compte. La version gratuite comprend un foyer, jusqu’à 30 objets et trois pièces jointes par objet. Premium, un achat unique facultatif, ajoute un nombre illimité d’objets et de foyers, l’OCR des reçus, des rappels avancés, l’entretien, les exports PDF/CSV/JSON, les recherches enregistrées, des commandes vocales étendues et les sauvegardes protégées par mot de passe. Aucun abonnement ; Amazon affiche le prix local. La vérification des achats nécessite internet ; l’accès signé hors ligne dure jusqu’à 90 jours avant renouvellement. La reconnaissance vocale dépend du fournisseur de l’appareil et peut utiliser un service réseau. Vérifiez les suggestions extraites des reçus avant de les appliquer. Les rapports sont des registres tenus par l’utilisateur, pas des évaluations certifiées par un assureur.',
 'store:iap_description':'Achat unique pour un nombre illimité d’objets et de foyers, l’OCR des reçus, les rappels avancés de garantie, l’entretien, les rapports PDF/CSV/JSON, les recherches enregistrées, les commandes vocales étendues et les sauvegardes protégées par mot de passe. Sans abonnement. La vérification de l’achat nécessite internet.',
 'support:getting_started':'Créez un foyer, choisissez sa devise et ajoutez des pièces. Ajoutez un objet avec ses détails d’achat et, si vous le souhaitez, la date d’expiration de sa garantie. Aucun compte n’est nécessaire.',
 'support:where_is_my_data':'Les enregistrements de l’inventaire et les pièces jointes restent dans le stockage privé de l’application sur cet appareil. Aucune synchronisation silencieuse n’est effectuée. La désinstallation supprime les données locales : enregistrez et vérifiez d’abord une sauvegarde.',
 'support:receipts':'Joignez une image ou un PDF du reçu, choisissez la page et la langue du reçu, puis recadrez ou faites pivoter l’image avant l’extraction du texte sur l’appareil. Vérifiez chaque suggestion avant de l’appliquer. La pièce jointe originale est conservée intégralement. Aucun changement de devise n’est déduit sans vous en informer.',
 'support:backup':'Dans les paramètres, enregistrez une sauvegarde ZIP contenant les métadonnées et les pièces jointes. Vous pouvez la protéger par un mot de passe d’au moins douze caractères. Conservez ce mot de passe en sécurité : il ne peut pas être récupéré. La restauration est limitée à 100 MiB après décompression ; chaque pièce jointe importée est limitée à 25 MiB. Les métadonnées de sauvegarde sont limitées à 8 MiB. Pendant la restauration, les pièces jointes sont placées temporairement dans le cache privé pour limiter l’utilisation de la mémoire.',
 'support:reminders':'Activez les rappels facultatifs dans les paramètres et autorisez les notifications à la demande. Possessory vérifie environ chaque jour les prochaines échéances de garantie et l’entretien planifié. La gestion de l’énergie de l’appareil peut retarder les notifications. Les rappels ne remplacent pas les inspections de sécurité professionnelles.',
 'support:voice':'Utilisez les phrases de commande affichées dans l’application et vérifiez la transcription. Les actions prises en charge comprennent la navigation, la recherche d’objets, leur comptage par pièce, la création d’un brouillon d’objet, la lecture des rappels de garantie et le déplacement d’un objet identifié sans ambiguïté après confirmation. Les actions étendues nécessitent Premium. Saisissez une commande si la reconnaissance vocale est indisponible ; le fournisseur vocal peut utiliser un service réseau.',
 'support:reports':'Choisissez une pièce, une catégorie ou un filtre de valeur dans une devise précise, puis un format de sortie. Vérifiez les options de numéros de série, valeurs enregistrées, photos et références de documents avant l’enregistrement. Parcourez le résumé paginé ou ouvrez l’aperçu PDF. Les rapports sont des inventaires tenus par l’utilisateur et non des évaluations certifiées par un assureur.',
 'support:delete':'Supprimez un objet depuis sa fiche pour retirer son enregistrement, ses pièces jointes et ses entrées d’entretien. Pour supprimer tous les enregistrements et paramètres locaux, choisissez Supprimer toutes les données locales dans les paramètres, puis confirmez. Les fichiers exportés ailleurs ne sont pas supprimés.',
 'support:app_lock':'Définissez un code PIN, un schéma ou un mot de passe de l’appareil, puis activez la protection par déverrouillage dans les paramètres. Possessory utilise l’écran d’authentification du système et ne conserve pas cet identifiant. Le verrouillage masque les aperçus des applications récentes et les captures d’écran. Cette protection est distincte du chiffrement des données stockées.',
})

FR.update(entries(r'''
home_office|Bureau à domicile
appliances|Électroménager
jewellery|Bijoux
art|Art
condition_new|Neuf
condition_good|Bon état
upgrade|Débloquer Premium
tagline|Tout ce que vous possédez. Bien organisé.
voice|Assistant vocal
add_first_item|Ajouter votre premier objet
room_name|Nom de la pièce
dashboard_subtitle|Une vue claire de tout ce qui compose votre foyer.
hero_title|Une place pour\ntout ce que vous possédez.
recorded_value|Valeur d’achat enregistrée
missing_receipts|Reçus manquants
empty_inventory_body|Ajoutez un objet pour rassembler ses détails, son reçu et sa garantie.
item_count|%1$d objets
search_hint|Rechercher des objets, marques, numéros de série et notes
all_rooms|Toutes les pièces
sort|Trier
active|Actif
room_required|Créez un foyer et une pièce avant d’ajouter des objets.
covered|Dans les dates de garantie enregistrées
household_name|Nom du foyer
currency|Devise
onboarding_privacy|Les fonctions principales sont disponibles hors ligne. Les photos et les documents restent dans le stockage privé de l’application. Les autorisations ne sont demandées que lorsque vous utilisez une fonction.
description|Description
status|Statut
scan_receipt|Vérifier et extraire les données du reçu
delete_item|Supprimer cet objet ?
review_receipt|Vérifier le reçu
receipt_preview|Aperçu du reçu
ocr_notice|Le texte du reçu est extrait sur cet appareil dans la langue sélectionnée. Choisissez une page du PDF, faites pivoter ou recadrez l’image et vérifiez chaque suggestion. Les pièces jointes originales sont conservées.
rotate|Faire pivoter
crop_edges|Rogner les bords
extract_text|Extraire le texte
mark_complete|Marquer comme terminé
reports_title|Votre foyer, en détail.
format|Format
inventory_report|Inventaire des biens du foyer
report_disclaimer|Inventaire tenu par l’utilisateur ; ce n’est pas une évaluation certifiée par un assureur.
settings_subtitle|Faites comme chez vous.
edit_household|Modifier le foyer
room_photo|Photo de la pièce
'''))
ES.update(entries(r'''
no_items_body|Añade un artículo o cambia la búsqueda y los filtros.
estimated_value|Tu estimación del valor actual
estimate_disclaimer|Tu estimación es independiente del precio de compra y no es una valoración profesional.
build_report|Prepara tu informe
voice_room_missing|No se encontró ninguna habitación coincidente. Selecciona una habitación en la pantalla.
warranty_manufacturer|Fabricante
app_locked|Possessory está bloqueado
delete_all_body|¿Eliminar permanentemente todos los hogares, artículos, archivos adjuntos y ajustes de este dispositivo? Las copias de seguridad que hayas exportado no se eliminan.
'''))

DE = entries(r'''
storage|Abstellraum
other|Sonstiges
appliances|Haushaltsgeräte
status_donated|Gespendet
status_lost|Verloren
upgrade|Premium freischalten
your_home|Ihr Zuhause
add_first_item|Ersten Gegenstand hinzufügen
room_name|Raumname
dashboard_subtitle|Ein klarer Überblick über Ihren Haushalt.
a_little_order|Ein wenig Ordnung. Viel mehr Gelassenheit.
hero_title|Ein Platz für\nalles, was Sie besitzen.
empty_inventory_body|Fügen Sie einen Gegenstand hinzu, um seine Angaben, Belege und Garantien zusammen aufzubewahren.
create_report|Inventarbericht erstellen
item_count|%1$d Gegenstände
your_belongings|Ihre Gegenstände
search_hint|Gegenstände, Marken, Seriennummern und Notizen suchen
no_items|Hier ist noch nichts
warranty_title|Ein gutes Gefühl.
warranty_subtitle|Ihre Garantieangaben immer griffbereit.
back|Zurück
create_home|Meinen Haushalt erstellen
warranty_notes|Notizen zur Garantie
add_receipt|Beleg anhängen
scan_receipt|Beleg prüfen und Daten auslesen
delete|Löschen
delete_item|Diesen Gegenstand löschen?
review_receipt|Beleg prüfen
receipt_preview|Belegvorschau
ocr_notice|Der Belegtext wird auf diesem Gerät in der ausgewählten Sprache ausgelesen. Wählen Sie eine PDF-Seite, drehen oder beschneiden Sie das Vorschaubild und prüfen Sie jeden Vorschlag. Die Originalanhänge bleiben erhalten.
rotate|Drehen
crop_edges|Ränder beschneiden
extract_text|Text auslesen
extracted_text|Ausgelesener Text
maintenance_title|Pflegen Sie, was Sie besitzen.
repeat_days|Wiederholungsintervall in Tagen (0 = einmalig)
mark_complete|Als erledigt markieren
reports_title|Ihr Zuhause, dokumentiert.
include_serials|Seriennummern einschließen
export_report|Bericht speichern
export_complete|Ihr Bericht wurde gespeichert.
report_preview|Berichtsvorschau
inventory_report|Haushaltsinventar
settings_subtitle|Richten Sie sich ein.
dark_theme|Dunkles Design
backup_body|Speichern Sie eine vollständige ZIP-Datei mit Datensätzen und Anhängen. Beim Wiederherstellen werden Prüfsummen geprüft und Einträge anhand ihrer ID zusammengeführt; vorhandene Datensätze bleiben erhalten. Erstellen Sie vor der Deinstallation eine Sicherung.
backup_password|Passwort (mindestens 12 Zeichen)
restore|Wiederherstellen
premium_unavailable|Käufe sind in dieser Entwicklungsvorschau nicht aktiviert. Die Einrichtung des Stores und der Kaufprüfung ist erforderlich. Das aktuelle Limit beträgt einen Haushalt und 30 Gegenstände; vorhandene Datensätze bleiben zugänglich.
listening|Ich höre zu… zum Beenden tippen
tap_to_speak|Zum Sprechen tippen
voice_command|Ihr Befehl
review_command|Befehl prüfen
voice_confirm|Der gewünschte Bildschirm wird geöffnet.
all_conditions|Alle Zustände
clear_filters|Filter zurücksetzen
household_currency_notice|Die Haushaltswährung wird für neue Datensätze verwendet. Währungen und Werte vorhandener Gegenstände bleiben erhalten.
unlock_notice|Verwenden Sie die PIN, das Muster oder das Passwort der Gerätesperre, um Possessory zu öffnen.
app_locked|Possessory ist gesperrt
app_lock_notice|Erfordert eine Geräte-PIN, ein Muster oder ein Passwort. Possessory speichert diese Zugangsdaten nicht. Die Sperre blendet auch Bildschirmfotos und die Vorschau zuletzt verwendeter Apps aus.
edit_maintenance|Wartung bearbeiten
custom_reminder_hint|Vorlaufzeiten sind Tage vor Ablauf, zum Beispiel 30,14,7,0. Zusätzliche Daten verwenden YYYY-MM-DD. Die Energiespareinstellungen des Geräts können tägliche Prüfungen verzögern.
reset_crop|Vorschaubild zurücksetzen
ocr_page|PDF-Seite
ocr_language|Sprache des Belegs
include_photos|Gegenstandsfotos einschließen
include_receipt_references|Anhangreferenzen einschließen
high_value|Gegenstände mit hohem Wert
value_threshold|Erfasster Mindestwert
column_serial|Seriennummer
voice_count_result|%d Gegenstände gehören zu diesem Raum.
voice_move_title|Verschieben des Gegenstands bestätigen
voice_room_missing|Kein passender Raum gefunden. Wählen Sie einen Raum auf dem Bildschirm aus.
voice_item_missing|Kein eindeutig passender Gegenstand gefunden. Wählen Sie ihn über die Suche aus.
review_mode_notice|TESTVERSION: Übersetzungsentwürfe und Bewertungswerkzeuge sind aktiviert. Es wird kein Kauf oder Premium-Nutzungsrecht gewährt. Nicht im Appstore veröffentlichen.
''')

IT = entries(r'''
home_office|Studio
storage|Ripostiglio
appliances|Elettrodomestici
tools|Attrezzi
condition_good|Buone condizioni
status_lost|Smarrito
add_room|Aggiungi stanza
room_name|Nome della stanza
dashboard_subtitle|Una visione chiara di tutto ciò che hai in casa.
a_little_order|Un po’ di ordine. Molta tranquillità.
missing_receipts|Ricevute mancanti
empty_inventory_body|Aggiungi un oggetto per conservare insieme i suoi dettagli, la ricevuta e la garanzia.
item_count|%1$d oggetti
your_belongings|I tuoi oggetti
sort|Ordina
archived|Archiviato
room_required|Crea una casa e una stanza prima di aggiungere oggetti.
warranty_title|Un po’ di tranquillità.
warranty_disclaimer|Le date di garanzia registrate sono fornite da te. Le garanzie del produttore e i diritti legali del consumatore sono distinti; la copertura dipende dalle condizioni applicabili.
description|Descrizione
estimate_disclaimer|La tua stima è distinta dal prezzo di acquisto e non è una valutazione professionale.
add_receipt|Allega ricevuta
no_attachments|Nessun allegato presente.
scan_receipt|Controlla ed estrai i dati della ricevuta
delete_item|Eliminare questo oggetto?
review_receipt|Controlla ricevuta
ocr_notice|Il testo della ricevuta viene estratto su questo dispositivo nella lingua scelta. Seleziona una pagina del PDF, ruota o ritaglia l’immagine e controlla ogni suggerimento. Gli allegati originali vengono conservati.
crop_edges|Ritaglia bordi
apply_details|Applica i dettagli verificati
maintenance_subtitle|Un ritmo utile per la cura della casa.
mark_complete|Segna come completato
reports_title|La tua casa, documentata.
all_items|Tutti gli oggetti attivi
inventory_report|Inventario degli oggetti della casa
settings_subtitle|Sentiti a casa.
dark_theme|Tema scuro
restore|Ripristina
premium_unavailable|Gli acquisti non sono attivi in questa versione di prova. Occorre configurare lo store e la verifica degli acquisti. Il limite attuale è una casa e 30 oggetti; i dati esistenti restano accessibili.
listening|In ascolto… tocca per interrompere
voice_unavailable|Il riconoscimento vocale o il comando non è disponibile. Digita uno dei comandi supportati qui sotto.
review_command|Controlla comando
voice_confirm|Apertura della schermata richiesta.
operation_error|L’operazione non è riuscita. Controlla il file, lo spazio disponibile, le autorizzazioni o i dati inseriti, quindi riprova. I dati esistenti sono stati conservati.
edit_household|Modifica casa
app_lock_notice|Richiede un PIN, una sequenza o una password del dispositivo. Possessory non conserva queste credenziali. Il blocco nasconde anche le schermate e le anteprime delle app recenti.
delete_maintenance_body|Verranno eliminati il registro di manutenzione e i relativi documenti. Gli altri registri degli oggetti saranno conservati.
reminder_offsets|Giorni di anticipo dei promemoria, separati da virgole
custom_reminder_hint|L’anticipo è espresso in giorni prima della scadenza, per esempio 30,14,7,0. Le date aggiuntive usano YYYY-MM-DD. Le impostazioni di risparmio energetico possono ritardare i controlli giornalieri.
reset_crop|Ripristina immagine da controllare
ocr_page|Pagina del PDF
ocr_language|Lingua della ricevuta
voice_count_result|%d oggetti corrispondono a questa stanza.
voice_move_title|Conferma spostamento dell’oggetto
voice_room_missing|Nessuna stanza corrispondente trovata. Seleziona una stanza sullo schermo.
review_mode_notice|VERSIONE DI VALUTAZIONE: traduzioni provvisorie e strumenti di valutazione attivi. Non viene concesso alcun acquisto né diritto a Premium. Non pubblicare su Appstore.
''')

DE.update({
 'store:title':'Possessory – Haushaltsinventar und Garantien',
 'store:description':'Ein Platz für alles, was Sie besitzen. Ordnen Sie Haushaltsgegenstände nach Räumen mit Fotos, Belegen, Kaufangaben und Garantiedaten. Durchsuchen Sie Ihre Einträge, verfolgen Sie Garantien und speichern Sie ZIP-Sicherungen auf Ihrem Gerät. Das grundlegende Inventar funktioniert offline und ohne Konto. Kostenlos enthalten sind ein Haushalt, bis zu 30 Gegenstände und drei Anhänge pro Gegenstand. Der optionale einmalige Premium-Kauf bietet unbegrenzt viele Gegenstände und Haushalte, Beleg-OCR, erweiterte Erinnerungen, Wartung, PDF/CSV/JSON-Exporte, gespeicherte Suchen, erweiterte Sprachbefehle und passwortgeschützte Sicherungen. Kein Abonnement; Amazon zeigt den örtlichen Preis. Die Kaufprüfung benötigt Internet. Signierter Offline-Zugang gilt bis zu 90 Tage vor der nächsten Aktualisierung. Die Spracherkennung hängt vom Geräteanbieter ab und kann einen Netzwerkdienst nutzen. Prüfen Sie Belegvorschläge vor dem Übernehmen. Berichte sind vom Nutzer gepflegte Inventaraufzeichnungen, keine vom Versicherer zertifizierten Bewertungen.',
 'support:title':'Possessory-Hilfe',
 'support:getting_started':'Erstellen Sie einen Haushalt, wählen Sie seine Währung und fügen Sie Räume hinzu. Erfassen Sie einen Gegenstand mit Kaufangaben und optionalem Garantieablaufdatum. Ein Konto ist nicht erforderlich.',
 'support:receipts':'Hängen Sie ein Belegbild oder PDF an, wählen Sie Seite und Belegsprache und beschneiden oder drehen Sie das Bild vor dem Auslesen auf dem Gerät. Prüfen Sie jeden Vorschlag vor dem Übernehmen. Der vollständige Originalanhang bleibt erhalten. Währungsänderungen werden niemals stillschweigend abgeleitet.',
 'support:backup':'Speichern Sie in den Einstellungen eine ZIP-Sicherung mit Metadaten und Anhängen. Optional können Sie ein Passwort mit mindestens zwölf Zeichen verwenden. Bewahren Sie es sicher auf; es kann nicht wiederhergestellt werden. Die Wiederherstellung ist auf 100 MiB nach dem Entpacken begrenzt; einzelne importierte Anhänge auf 25 MiB und Sicherungsmetadaten auf 8 MiB. Anhänge werden beim Wiederherstellen vorübergehend im privaten Cache abgelegt, um den Arbeitsspeicherbedarf zu begrenzen.',
 'support:reminders':'Aktivieren Sie optionale Erinnerungen in den Einstellungen und erlauben Sie Benachrichtigungen, wenn Sie gefragt werden. Possessory prüft anstehende Garantiefristen und geplante Wartungen ungefähr täglich. Die Energieverwaltung des Geräts kann Benachrichtigungen verzögern. Erinnerungen ersetzen keine professionellen Sicherheitsprüfungen.',
 'support:voice':'Verwenden Sie die in der App gezeigten Befehle und prüfen Sie die erkannte Eingabe. Unterstützt werden Navigation, Gegenstandssuche, Zählen der Gegenstände eines Raums, Erstellen eines Gegenstandsentwurfs, Vorlesen von Garantieerinnerungen und Verschieben eines eindeutig erkannten Gegenstands nach Bestätigung. Erweiterte Aktionen benötigen Premium. Tippen Sie den Befehl ein, wenn Spracherkennung nicht verfügbar ist; der Sprachanbieter kann einen Netzwerkdienst nutzen.',
 'support:reports':'Wählen Sie einen Raum, eine Kategorie oder einen Wertfilter für eine bestimmte Währung sowie das Ausgabeformat. Prüfen Sie vor dem Speichern die Optionen für Seriennummern, erfasste Werte, Fotos und Dokumentreferenzen. Blättern Sie durch die Zusammenfassung oder öffnen Sie die PDF-Vorschau. Berichte sind vom Nutzer gepflegte Inventaraufzeichnungen und keine vom Versicherer zertifizierten Bewertungen.',
 'support:warranties':'Sie geben Garantiedaten und Bedingungen selbst an. Herstellergarantien und gesetzliche Verbraucherrechte sind getrennt. Possessory bestimmt keine Anspruchsberechtigung und erteilt keine verbindliche Rechtsberatung.',
 'support:delete':'Löschen Sie einen Gegenstand in seiner Detailansicht, um seinen Datensatz, seine Anhänge und Wartungseinträge zu entfernen. Um alle lokalen Datensätze und Einstellungen zu entfernen, wählen Sie in den Einstellungen Alle lokalen Daten löschen und bestätigen Sie. Anderswohin exportierte Dateien werden nicht gelöscht.',
 'support:contact_label':'Support kontaktieren',
 'support:app_lock':'Legen Sie eine Geräte-PIN, ein Muster oder ein Passwort fest und aktivieren Sie dann den Entsperrschutz in den Einstellungen. Possessory verwendet den Anmeldebildschirm des Systems und speichert diese Zugangsdaten nicht. Die Sperre verbirgt die Vorschau zuletzt verwendeter Apps und Bildschirmfotos. Sie ist von der Verschlüsselung gespeicherter Daten getrennt.',
})
IT.update({
 'store:title':'Possessory – Inventario della casa e garanzie',
 'store:description':'Un posto per tutto ciò che possiedi. Organizza gli oggetti di casa per stanza con foto, ricevute, dettagli di acquisto e date di garanzia. Cerca nei registri, controlla le garanzie e salva backup ZIP sul dispositivo. L’inventario di base funziona offline e non richiede un account. La versione gratuita comprende una casa, fino a 30 oggetti e tre allegati per oggetto. Premium, un acquisto unico facoltativo, aggiunge oggetti e case illimitati, OCR delle ricevute, promemoria avanzati, manutenzione, esportazioni PDF/CSV/JSON, ricerche salvate, comandi vocali estesi e backup protetti da password. Nessun abbonamento; Amazon mostra il prezzo locale. La verifica dell’acquisto richiede internet; l’accesso offline firmato dura fino a 90 giorni prima del rinnovo. Il riconoscimento vocale dipende dal fornitore del dispositivo e può usare un servizio di rete. Controlla i suggerimenti della ricevuta prima di applicarli. I report sono registri gestiti dall’utente, non valutazioni certificate da un assicuratore.',
 'support:title':'Assistenza Possessory',
 'support:getting_started':'Crea una casa, scegli la valuta e aggiungi le stanze. Aggiungi un oggetto con i dettagli di acquisto e, facoltativamente, la scadenza della garanzia. Non è necessario un account.',
 'support:receipts':'Allega un’immagine o un PDF della ricevuta, scegli la pagina e la lingua, poi ritaglia o ruota l’immagine prima di estrarre il testo sul dispositivo. Controlla ogni suggerimento prima di applicarlo. L’allegato originale viene conservato integralmente. I cambi di valuta non vengono mai dedotti senza avvisarti.',
 'support:backup':'Nelle impostazioni, salva un backup ZIP con metadati e allegati. Puoi proteggerlo con una password di almeno dodici caratteri. Conservala al sicuro: non può essere recuperata. Il ripristino è limitato a 100 MiB dopo la decompressione; ogni allegato importato è limitato a 25 MiB e i metadati del backup a 8 MiB. Durante il ripristino gli allegati vengono temporaneamente salvati nella cache privata per limitare l’uso della memoria.',
 'support:restore':'Apri un backup nelle impostazioni, inserisci la password se è cifrato e controlla l’anteprima di importazione. Possessory verifica la struttura, gli hash degli allegati e i riferimenti dei registri. Gli ID esistenti vengono mantenuti e si aggiungono solo nuovi registri. Dopo il ripristino controlla gli oggetti e gli allegati.',
 'support:reminders':'Attiva i promemoria facoltativi nelle impostazioni e consenti le notifiche quando richiesto. Possessory controlla circa ogni giorno le prossime scadenze di garanzia e la manutenzione programmata. Il risparmio energetico del dispositivo può ritardare le notifiche. I promemoria non sostituiscono le ispezioni di sicurezza professionali.',
 'support:voice':'Usa le frasi di comando mostrate nell’app e controlla la trascrizione. Le azioni supportate includono navigazione, ricerca di oggetti, conteggio degli oggetti per stanza, creazione di una bozza di oggetto, lettura dei promemoria di garanzia e spostamento di un oggetto identificato senza ambiguità dopo conferma. Le azioni estese richiedono Premium. Digita il comando se il riconoscimento vocale non è disponibile; il fornitore vocale può usare un servizio di rete.',
 'support:reports':'Scegli una stanza, una categoria o un filtro di valore per una valuta specifica e un formato di uscita. Controlla le opzioni per numeri di serie, valori registrati, foto e riferimenti ai documenti prima di salvare. Sfoglia il riepilogo suddiviso in pagine o apri l’anteprima PDF. I report sono inventari gestiti dall’utente, non valutazioni certificate da un assicuratore.',
 'support:delete':'Elimina un oggetto dalla sua schermata dei dettagli per rimuovere il registro, gli allegati e le voci di manutenzione. Per eliminare tutti i dati e le impostazioni locali, scegli Cancella tutti i dati locali nelle impostazioni e conferma. I file esportati altrove non vengono eliminati.',
 'support:contact_label':'Contatta l’assistenza',
})

PT = entries(r'''
condition_good|Bom estado
add_household|Adicionar casa
household_name|Nome da casa
description|Descrição
serial|Número de série
column_serial|Número de série
scan_receipt|Rever e extrair dados do recibo
review_receipt|Rever recibo
receipt_preview|Pré-visualização do recibo
mark_complete|Marcar como concluído
reports_title|A sua casa, documentada.
report_preview|Pré-visualização do relatório
inventory_report|Inventário dos bens da casa
premium|Possessory Premium
tap_to_speak|Toque para falar
review_command|Rever comando
delete_maintenance_body|O registo de manutenção e os documentos associados serão removidos. Os restantes registos de artigos serão mantidos.
reminder_offsets|Dias de antecedência dos lembretes, separados por vírgulas
custom_reminder_hint|A antecedência é indicada em dias antes do fim da garantia, por exemplo 30,14,7,0. As datas adicionais usam YYYY-MM-DD. As definições de energia do dispositivo podem atrasar as verificações diárias.
ocr_page|Página do PDF
ocr_language|Idioma do recibo
column_purchase_amount|Montante da compra
voice_count_result|%d pertences correspondem a esta divisão.
voice_room_missing|Não foi encontrada uma divisão correspondente. Selecione uma divisão no ecrã.
voice_item_missing|Não foi encontrado um artigo correspondente único. Use a pesquisa para selecionar o artigo.
review_mode_notice|VERSÃO DE AVALIAÇÃO: traduções provisórias e ferramentas de avaliação ativas. Não é concedida qualquer compra nem acesso Premium. Não publicar na Appstore.
premium_unavailable|As compras não estão disponíveis nesta versão de teste. É necessário configurar a loja e a verificação de compras. O limite atual é uma casa e 30 artigos; os registos existentes continuam acessíveis.
''')
PTPT = {**PT, **entries(r'''
your_home|A sua casa
room_name|Nome da divisão
all_rooms|Todas as divisões
room_required|Crie uma casa e uma divisão antes de adicionar pertences.
hero_title|Um lugar para\ntudo o que possui.
inventory_subtitle|Os objetos que fazem da sua casa um lar.
onboarding_privacy|As funções principais funcionam sem internet. As fotografias e os documentos ficam no armazenamento privado da aplicação. As permissões só são pedidas ao utilizar uma função.
covered|Dentro das datas de garantia registadas
ocr_notice|O texto do recibo é extraído neste dispositivo no idioma selecionado. Escolha uma página do PDF, rode ou recorte a imagem de revisão e verifique cada sugestão. Os anexos originais são conservados.
export_report|Guardar relatório
export_complete|O relatório foi guardado.
encrypted_backup|Cópia de segurança protegida por palavra-passe
backup_password|Palavra-passe (pelo menos 12 caracteres)
backup_complete|Operação de cópia de segurança concluída.
restore_preview|Rever a cópia de segurança antes de restaurar
saved_searches|Pesquisas guardadas
manage_searches|Gerir pesquisas guardadas
unlock_notice|Use o PIN, padrão ou palavra-passe de bloqueio do dispositivo para abrir Possessory.
app_lock_notice|Requer um PIN, padrão ou palavra-passe do dispositivo. Possessory não guarda essa credencial. O bloqueio também oculta capturas de ecrã e pré-visualizações das aplicações recentes.
device_lock_required|Configure um bloqueio de ecrã nas definições do sistema antes de ativar o bloqueio da aplicação.
room_photo|Fotografia da divisão
''')}
PTBR = {**PT, **entries(r'''
bathroom|Banheiro
garage|Garagem
electronics|Eletrônicos
furniture|Móveis
status_sold|Vendido
your_home|Sua casa
room_name|Nome do cômodo
all_rooms|Todos os cômodos
room_required|Crie uma casa e um cômodo antes de adicionar pertences.
recorded_value|Valor de compra registrado
missing_receipts|Recibos ausentes
back|Voltar
warranty_start|Início da garantia
warranty_provider|Fornecedor da garantia
no_attachments|Ainda não há anexos.
ocr_notice|O texto do recibo é extraído neste dispositivo no idioma selecionado. Escolha uma página do PDF, gire ou recorte a imagem de revisão e verifique cada sugestão. Os anexos originais são mantidos.
ocr_unavailable|Não foi possível concluir a extração. Você pode inserir os detalhes manualmente e manter o anexo.
repeat_days|Intervalo de repetição em dias (0 = uma vez)
reports_title|Sua casa, documentada.
export_report|Salvar relatório
backup_password|Senha (pelo menos 12 caracteres)
premium_unavailable|As compras não estão disponíveis nesta versão de teste. É necessário configurar a loja e a verificação de compras. O limite atual é uma casa e 30 itens; os registros existentes continuam acessíveis.
delete_all_body|Remover permanentemente todas as casas, itens, anexos e configurações deste dispositivo? Os backups que você exportou não serão apagados.
voice_unavailable|O reconhecimento de voz ou o comando não está disponível. Digite um dos comandos aceitos abaixo.
operation_error|A operação falhou. Verifique o arquivo, o espaço disponível, as permissões ou os dados inseridos e tente novamente. Seus registros existentes foram mantidos.
unlock_notice|Use o PIN, padrão ou senha de bloqueio do dispositivo para abrir Possessory.
app_locked|Possessory está bloqueado
app_lock_notice|Requer um PIN, padrão ou senha do dispositivo. Possessory não armazena essa credencial. O bloqueio também oculta capturas de tela e prévias dos aplicativos recentes.
delete_maintenance_body|O registro de manutenção e os documentos associados serão removidos. Os outros registros de itens serão mantidos.
custom_reminder_hint|A antecedência é indicada em dias antes do fim da garantia, por exemplo 30,14,7,0. As datas adicionais usam YYYY-MM-DD. As configurações de energia do dispositivo podem atrasar as verificações diárias.
column_warranty|Vencimento da garantia
voice_count_result|%d pertences correspondem a este cômodo.
voice_room_missing|Nenhum cômodo correspondente foi encontrado. Selecione um cômodo na tela.
voice_item_missing|Nenhum item correspondente único foi encontrado. Use a busca para selecionar o item.
''')}

PT_CONTENT = {
 'store:title':'Possessory – Inventário da casa e garantias',
 'store:iap_title':'Possessory Premium',
 'store:description':'Um lugar para tudo o que possui. Organize os bens da casa por divisão, com fotografias, recibos, dados de compra e datas de garantia. Pesquise registos, acompanhe garantias e guarde cópias de segurança ZIP no dispositivo. O inventário básico funciona sem internet e não exige conta. A versão gratuita inclui uma casa, até 30 artigos e três anexos por artigo. A compra única opcional Premium acrescenta artigos e casas ilimitados, OCR de recibos, lembretes avançados, manutenção, exportações PDF/CSV/JSON, pesquisas guardadas, comandos de voz alargados e cópias de segurança com palavra-passe. Sem subscrição; a Amazon mostra o preço local. A verificação da compra exige internet; o acesso assinado sem ligação dura até 90 dias antes de ser renovado. O reconhecimento de voz depende do fornecedor do dispositivo e pode usar um serviço de rede. Reveja as sugestões do recibo antes de as aplicar. Os relatórios são registos mantidos pelo utilizador, não avaliações certificadas por uma seguradora.',
 'store:iap_description':'Compra única para artigos e casas ilimitados, OCR de recibos, lembretes avançados de garantia, manutenção, relatórios PDF/CSV/JSON, pesquisas guardadas, comandos de voz alargados e cópias de segurança com palavra-passe. Sem subscrição. A verificação da compra exige internet.',
 'support:title':'Apoio Possessory',
 'support:getting_started':'Crie uma casa, escolha a moeda e adicione divisões. Adicione um artigo com os dados de compra e, opcionalmente, a data de fim da garantia. Não é necessária uma conta.',
 'support:where_is_my_data':'Os registos do inventário e os anexos ficam no armazenamento privado da aplicação neste dispositivo. Nada é sincronizado silenciosamente. A desinstalação remove os dados locais; guarde e verifique primeiro uma cópia de segurança.',
 'support:receipts':'Anexe uma imagem ou PDF do recibo, escolha a página e o idioma e recorte ou rode a imagem antes de extrair o texto no dispositivo. Verifique todas as sugestões antes de as aplicar. O anexo original é conservado na íntegra. As alterações de moeda nunca são inferidas sem aviso.',
 'support:backup':'Nas Definições, guarde uma cópia de segurança ZIP com metadados e anexos. Pode protegê-la com uma palavra-passe de pelo menos doze caracteres. Guarde-a em segurança: não pode ser recuperada. O limite de restauro é de 100 MiB após descompressão; cada anexo importado está limitado a 25 MiB e os metadados a 8 MiB. Os anexos são guardados temporariamente na cache privada durante o restauro para limitar o uso da memória.',
 'support:restore':'Abra uma cópia de segurança nas Definições, introduza a palavra-passe se estiver cifrada e reveja a pré-visualização da importação. Possessory verifica a estrutura, os hashes dos anexos e as referências dos registos. Os identificadores existentes são mantidos e só são acrescentados novos registos. Verifique os artigos e anexos após o restauro.',
 'support:reminders':'Ative os lembretes opcionais nas Definições e permita notificações quando solicitado. Possessory verifica aproximadamente todos os dias as próximas datas de garantia e a manutenção planeada. A gestão de energia do dispositivo pode atrasar as notificações. Os lembretes não substituem inspeções profissionais de segurança.',
 'support:voice':'Use as frases de comando apresentadas na aplicação e reveja a transcrição. As ações incluem navegação, pesquisa de artigos, contagem de artigos por divisão, criação de um rascunho de artigo, leitura de lembretes de garantia e deslocação de um artigo identificado sem ambiguidade após confirmação. As ações alargadas exigem Premium. Escreva o comando se o reconhecimento de voz não estiver disponível; o fornecedor de voz pode usar um serviço de rede.',
 'support:reports':'Escolha uma divisão, categoria ou filtro de valor para uma moeda específica e um formato de saída. Reveja as opções de números de série, valores registados, fotografias e referências de documentos antes de guardar. Consulte o resumo paginado ou a pré-visualização PDF. Os relatórios são inventários mantidos pelo utilizador, não avaliações certificadas por uma seguradora.',
 'support:delete':'Apague um artigo na sua página de detalhes para remover o registo, anexos e entradas de manutenção. Para remover todos os dados e definições locais, escolha Apagar todos os dados locais nas Definições e confirme. Os ficheiros exportados para outro local não são apagados.',
 'support:contact_label':'Contactar o apoio',
 'support:app_lock':'Configure um PIN, padrão ou palavra-passe do dispositivo e ative a proteção por desbloqueio nas Definições. Possessory usa o ecrã de credenciais do sistema e não guarda essa credencial. O bloqueio oculta as pré-visualizações de aplicações recentes e as capturas de ecrã. Esta proteção é distinta da cifragem dos dados armazenados.',
}
PTPT.update(PT_CONTENT)
for key, value in PT_CONTENT.items():
    for old,new in [('Apoio Possessory','Suporte Possessory'),('Contactar o apoio','Contatar o suporte'),('Definições','Configurações'),('definições','configurações'),('palavra-passe','senha'),('cópias de segurança','backups'),('cópia de segurança','backup'),('divisões','cômodos'),('divisão','cômodo'),('registos','registros'),('registo','registro'),('artigos','itens'),('artigo','item'),('utilizador','usuário'),('aplicações','aplicativos'),('aplicação','aplicativo'),('fotografias','fotos'),('subscrição','assinatura'),('guardadas','salvas'),('guarde','salve'),('guardar','salvar'),('guardados','salvos'),('guardado','salvo'),('conservado','mantido'),('restauro','restauração'),('cifrada','criptografado'),('cifragem','criptografia'),('ficheiros','arquivos'),('ecrã','tela'),('alargados','ampliados'),('alargadas','ampliadas'),('planeada','planejada'),('introduza','digite'),('deslocação','movimentação')]:
        value=value.replace(old,new)
    PTBR[key]=value.replace('uma backup','um backup').replace('protegê-la','protegê-lo')

NL = entries(r'''
kitchen|Keuken
home_office|Werkkamer
tools|Gereedschap
recorded_value|Vastgelegde aankoopwaarde
empty_inventory_body|Voeg een voorwerp toe om de gegevens, bon en garantie bij elkaar te bewaren.
serial|Serienummer
column_serial|Serienummer
decimal_hint|Voer een bedrag in met het decimaalteken van de gekozen regio, zonder valutateken of scheidingstekens voor duizendtallen.
receipts_documents|Foto’s, bonnen en documenten
add_receipt|Bon bijvoegen
scan_receipt|Bon controleren en gegevens uitlezen
duplicate|Dupliceren
review_receipt|Bon controleren
receipt_preview|Voorbeeld van de bon
ocr_notice|De bontekst wordt op dit apparaat uitgelezen in de gekozen taal. Kies een PDF-pagina, draai of snijd de afbeelding bij en controleer elk voorstel. De originele bijlagen blijven behouden.
mark_complete|Markeren als voltooid
reports_title|Uw huis, vastgelegd.
premium|Possessory Premium
review_command|Opdracht controleren
voice_confirm|Het gevraagde scherm wordt geopend.
all_conditions|Alle staten
unlock_notice|Gebruik de pincode, het patroon of het wachtwoord van uw schermvergrendeling om Possessory te openen.
app_lock_notice|Vereist een pincode, patroon of wachtwoord van het apparaat. Possessory slaat deze ontgrendelgegevens niet op. Vergrendeling verbergt ook schermafbeeldingen en voorbeelden van recente apps.
custom_reminder_hint|De termijn is het aantal dagen vóór het verlopen, bijvoorbeeld 30,14,7,0. Extra datums gebruiken YYYY-MM-DD. Energiebesparende instellingen kunnen de dagelijkse controles vertragen.
reset_crop|Controleafbeelding herstellen
ocr_language|Taal van de bon
column_item|Voorwerp
voice_room_missing|Geen bijpassende kamer gevonden. Selecteer een kamer op het scherm.
''')
NL['privacy_body']=r'''Inventarisgegevens, huishoudens, kamerfoto’s, bonnen, onderhoudsdocumenten en voorkeuren staan in de privéopslag van de app. Er is geen account nodig en er zijn geen advertenties of analyses. Er wordt niets stilzwijgend gesynchroniseerd. OCR van bonnen gebeurt lokaal in de gekozen taal.\n\nSpraakinvoer is optioneel en kan de netwerkdienst van de spraakaanbieder op het apparaat gebruiken. Possessory bewaart geen ruwe opnamen. Foto’s, bonnen, notities en serienummers worden niet naar de aankoopverificatie gestuurd.\n\nWanneer aankopen zijn ingesteld, worden Amazon-gebruikers-ID’s en aankoopbon-ID’s via HTTPS naar de Possessory-verificatieservice op Cloudflare Workers gestuurd om aankopen te controleren en toegang te herstellen. De service gebruikt het IP-adres van de verbinding en hashes van aankoop-ID’s om verzoeken kortstondig te begrenzen. Er is geen klantendatabase en het loggen van appverzoeken staat uit. Cloudflare verwerkt netwerkmetadata om de dienst te beheren en te beveiligen. Amazon verwerkt de betaling afzonderlijk. Geverifieerde offline toegang kan tot 90 dagen geldig blijven, of totdat een eerdere geverifieerde annulering ingaat; daarna is online vernieuwing nodig.\n\nExports en back-ups worden alleen opgeslagen op een door u gekozen bestemming, mogelijk bij een clouddocumentaanbieder. Gewone ZIP-back-ups zijn niet versleuteld. Met een wachtwoord beveiligde back-ups gebruiken AES-GCM; wachtwoorden kunnen niet worden teruggehaald. Apparaatontgrendeling is optioneel en versleutelt de database en bijlagen niet afzonderlijk.\n\nVerwijder afzonderlijke gegevens of alle lokale gegevens via Instellingen. Bij het verwijderen van de app verdwijnen de privégegevens; geëxporteerde kopieën blijven op hun opslaglocatie. Bewaar een gecontroleerde back-up voordat u de app verwijdert. Neem contact op met de ondersteuning om privacy- of ondersteuningscorrespondentie te laten verwijderen.'''

SV = entries(r'''
bedroom|Sovrum
home_office|Hemmakontor
furniture|Möbler
voice|Röstassistent
recorded_value|Registrerat inköpsvärde
recently_added|Nyligen tillagt
newest|Nyligen tillagt
empty_inventory_body|Lägg till ett föremål för att samla uppgifter, kvitto och garanti på ett ställe.
name|Namn
warranty_disclaimer|Du anger garantidatum själv. Tillverkarens garantier och lagstadgade konsumenträttigheter är separata; rätten till ersättning beror på tillämpliga villkor.
edit_item|Redigera föremål
review_receipt|Granska kvitto
receipt_preview|Förhandsgranska kvitto
ocr_notice|Kvittotexten extraheras på den här enheten på det valda språket. Välj en PDF-sida, rotera eller beskär bilden och kontrollera varje förslag. Originalbilagorna behålls.
crop_edges|Beskär kanter
extract_text|Extrahera text
ocr_unavailable|Texten kunde inte extraheras. Du kan fortfarande ange uppgifterna manuellt och behålla bilagan.
maintenance_subtitle|En bra rutin för att sköta hemmet.
no_maintenance|En nystart för underhållet
cost|Registrerad underhållskostnad
mark_complete|Markera som slutförd
reports_title|Ditt hem, dokumenterat.
reports_subtitle|En tydlig inventarieförteckning, redo när du behöver den.
report_disclaimer|Inventarieförteckning som sköts av användaren; ingen värdering certifierad av ett försäkringsbolag.
dark_theme|Mörkt tema
backup_body|Spara en fullständig ZIP med poster och bilagor. Vid återställning kontrolleras kontrollsummor och poster sammanfogas utifrån sina ID:n; befintliga poster behålls vid konflikter. Spara en säkerhetskopia innan du avinstallerar.
premium_unavailable|Köp är inte aktiverade i den här testversionen. Butiken och köpverifieringen måste konfigureras. Den nuvarande gränsen är ett hushåll och 30 föremål; befintliga poster är fortfarande tillgängliga.
listening|Lyssnar… tryck för att stoppa
voice_confirm|Den begärda skärmen öppnas.
all_conditions|Alla skick
clear_filters|Rensa filter
unlock_notice|Använd enhetens PIN-kod, mönster eller lösenord för att öppna Possessory.
app_lock_notice|Kräver enhetens PIN-kod, mönster eller lösenord. Possessory lagrar inte dessa uppgifter. Låsningen döljer också skärmbilder och förhandsvisningar av nyligen använda appar.
custom_reminder_hint|Ange antal dagar före utgångsdatumet, till exempel 30,14,7,0. Extra datum använder YYYY-MM-DD. Enhetens energisparinställningar kan fördröja dagliga kontroller.
reset_crop|Återställ granskningsbilden
ocr_language|Kvittots språk
receipt_reference_note|Bilagereferenser identifierar lokala dokument; filerna bäddas inte in i rapporten.
voice_move_title|Bekräfta flytt av föremål
voice_room_missing|Inget matchande rum hittades. Välj ett rum på skärmen.
review_mode_notice|UTVÄRDERINGSVERSION: översättningsutkast och utvärderingsverktyg är aktiverade. Inget köp eller rätt till Premium beviljas. Får inte publiceras i Appstore.
''')

NL.update({
 'store:title':'Possessory – Huisinventaris en garanties',
 'store:iap_title':'Possessory Premium',
 'store:description':'Een plek voor alles wat u bezit. Orden huishoudelijke bezittingen per kamer met foto’s, bonnen, aankoopgegevens en garantiedatums. Zoek in uw gegevens, volg garanties en bewaar ZIP-back-ups op uw apparaat. De basisinventaris werkt offline en zonder account. Gratis omvat één huishouden, maximaal 30 voorwerpen en drie bijlagen per voorwerp. Een optionele eenmalige Premium-aankoop voegt onbeperkte voorwerpen en huishoudens toe, plus bon-OCR, uitgebreide herinneringen, onderhoud, PDF/CSV/JSON-export, opgeslagen zoekopdrachten, uitgebreide spraakopdrachten en back-ups met wachtwoord. Geen abonnement; Amazon toont de lokale prijs. Aankoopverificatie vereist internet; ondertekende offline toegang duurt maximaal 90 dagen tot vernieuwing. Spraakherkenning hangt af van de aanbieder op het apparaat en kan een netwerkdienst gebruiken. Controleer voorstellen uit bonnen voordat u ze toepast. Rapporten zijn door de gebruiker bijgehouden inventarissen, geen door een verzekeraar gecertificeerde taxaties.',
 'support:getting_started':'Maak een huishouden aan, kies de valuta en voeg kamers toe. Voeg een voorwerp toe met aankoopgegevens en eventueel de einddatum van de garantie. Een account is niet nodig.',
 'support:where_is_my_data':'Inventarisgegevens en bijlagen staan in de privéopslag van de app op dit apparaat. Er wordt niets stilzwijgend gesynchroniseerd. Bij het verwijderen van de app verdwijnen lokale gegevens; bewaar en controleer daarom eerst een back-up.',
 'support:receipts':'Voeg een bonafbeelding of PDF toe, kies een pagina en de taal van de bon en snijd of draai de afbeelding voordat de tekst op het apparaat wordt uitgelezen. Controleer elk voorstel voordat u het toepast. De volledige originele bijlage blijft behouden. Valutawijzigingen worden nooit stilzwijgend afgeleid.',
 'support:backup':'Bewaar in Instellingen een ZIP-back-up met metadata en bijlagen. U kunt wachtwoordbeveiliging kiezen met minimaal twaalf tekens. Bewaar het wachtwoord veilig; het kan niet worden teruggehaald. Herstel is beperkt tot 100 MiB na uitpakken, afzonderlijke geïmporteerde bijlagen tot 25 MiB en back-upmetadata tot 8 MiB. Bijlagen worden tijdens herstel tijdelijk in de privécache geplaatst om geheugengebruik te beperken.',
 'support:voice':'Gebruik de opdrachtzinnen in de app en controleer de herkende tekst. Ondersteunde acties zijn navigeren, voorwerpen zoeken, voorwerpen per kamer tellen, een conceptvoorwerp maken, garantieherinneringen voorlezen en een uniek gevonden voorwerp verplaatsen na bevestiging. Uitgebreide acties vereisen Premium. Typ een opdracht als spraakherkenning niet beschikbaar is; de spraakaanbieder kan een netwerkdienst gebruiken.',
 'support:reports':'Kies een kamer, categorie of waardefilter voor een bepaalde valuta en een uitvoerformaat. Controleer de opties voor serienummers, vastgelegde waarden, foto’s en documentverwijzingen voordat u opslaat. Blader door de samenvatting of open het PDF-voorbeeld. Rapporten zijn door de gebruiker bijgehouden inventarissen, geen door een verzekeraar gecertificeerde taxaties.',
 'support:warranties':'U geeft de garantiedatums en voorwaarden op. Fabrieksgaranties en wettelijke consumentenrechten staan los van elkaar. Possessory bepaalt niet of u aanspraak op garantie hebt en geeft geen definitief juridisch advies.',
 'support:app_lock':'Stel een pincode, patroon of wachtwoord voor het apparaat in en schakel daarna ontgrendelbeveiliging in bij Instellingen. Possessory gebruikt het verificatiescherm van het systeem en slaat de ontgrendelgegevens niet op. Vergrendeling verbergt voorbeelden van recente apps en schermafbeeldingen. Dit staat los van versleuteling van opgeslagen gegevens.',
})
SV.update({
 'store:title':'Possessory – Heminventarie och garantier',
 'store:description':'En plats för allt du äger. Ordna hemmets föremål efter rum med foton, kvitton, inköpsuppgifter och garantidatum. Sök bland poster, följ garantier och spara ZIP-säkerhetskopior på enheten. Grundfunktionerna fungerar offline utan konto. Gratisversionen omfattar ett hushåll, upp till 30 föremål och tre bilagor per föremål. Ett valfritt engångsköp av Premium ger obegränsade föremål och hushåll, kvitto-OCR, avancerade påminnelser, underhåll, PDF/CSV/JSON-export, sparade sökningar, utökade röstkommandon och lösenordsskyddade säkerhetskopior. Ingen prenumeration; Amazon visar det lokala priset. Köpverifiering kräver internet. Signerad offlineåtkomst gäller i upp till 90 dagar innan den behöver förnyas. Taligenkänning beror på enhetens leverantör och kan använda en nätverkstjänst. Kontrollera förslag från kvitton innan du tillämpar dem. Rapporterna är inventarieförteckningar som användaren sköter, inte värderingar certifierade av ett försäkringsbolag.',
 'store:iap_description':'Engångsköp för obegränsade föremål och hushåll, kvitto-OCR, avancerade garantipåminnelser, underhåll, PDF/CSV/JSON-rapporter, sparade sökningar, utökade röstkommandon och lösenordsskyddade säkerhetskopior. Ingen prenumeration. Köpverifiering kräver internet.',
 'support:title':'Possessory-support',
 'support:where_is_my_data':'Inventarieposter och bilagor lagras i appens privata lagring på enheten. Ingenting synkroniseras i det tysta. Avinstallation tar bort lokala data, så spara och kontrollera en säkerhetskopia först.',
 'support:receipts':'Bifoga en kvittobild eller PDF, välj sida och kvittots språk och beskär eller rotera bilden innan texten extraheras på enheten. Kontrollera varje förslag innan du tillämpar det. Hela originalbilagan behålls. Valutaändringar härleds aldrig utan att du informeras.',
 'support:backup':'Spara en ZIP-säkerhetskopia med metadata och bilagor i Inställningar. Du kan välja lösenordsskydd med minst tolv tecken. Förvara lösenordet säkert; det kan inte återskapas. Återställning är begränsad till 100 MiB efter uppackning; enskilda importerade bilagor till 25 MiB och metadata till 8 MiB. Bilagor lagras tillfälligt i den privata cachen under återställning för att begränsa minnesanvändningen.',
 'support:premium':'Premium är ett engångsköp via Amazon utan prenumeration. Butiken visar det lokala priset. Köp och återställning kräver internet. Amazon-användar-ID och köpets kvitto-ID skickas via HTTPS till Possessory-verifieraren på Cloudflare Workers, som kontrollerar köpet hos Amazon och returnerar en signerad rättighet. Heminventarier och fotograferade kvitton skickas inte. Verifieraren använder anslutningens IP-adress och hashvärden av köp-ID:n för kortvarig begränsning av antalet anrop. Den har ingen kunddatabas och loggning av appförfrågningar är avstängd. Cloudflare behandlar nätverksmetadata för att driva och skydda tjänsten. Signerad offlineåtkomst gäller i upp till 90 dagar, eller tills en tidigare verifierad annullering träder i kraft, innan den behöver förnyas. Befintliga poster, vanlig redigering, radering och återställning av säkerhetskopior är tillgängliga utan Premium.',
 'support:voice':'Använd kommandofraserna i appen och granska den igenkända texten. Du kan navigera, söka föremål, räkna föremål per rum, skapa ett föremålsutkast, läsa upp garantipåminnelser och flytta ett entydigt identifierat föremål efter bekräftelse. Utökade åtgärder kräver Premium. Skriv kommandot om taligenkänning saknas; talleverantören kan använda en nätverkstjänst.',
 'support:warranties':'Du anger garantidatum och villkor. Tillverkargarantier och lagstadgade konsumenträttigheter är separata. Possessory avgör inte om du har rätt till ersättning och ger inte bindande juridisk rådgivning.',
 'support:app_lock':'Ange enhetens PIN-kod, mönster eller lösenord och aktivera sedan upplåsningsskyddet i Inställningar. Possessory använder systemets verifieringsskärm och sparar inte dessa uppgifter. Låsningen döljer förhandsvisningar av nyligen använda appar och skärmbilder. Detta är skilt från kryptering av lagrade data.',
})

PL = entries(r'''
home_office|Gabinet domowy
condition_good|Dobry
status_donated|Oddany
upgrade|Odblokuj Premium
tagline|Wszystko, co posiadasz. Starannie uporządkowane.
voice|Asystent głosowy
missing_receipts|Brakujące paragony
empty_inventory_body|Dodaj przedmiot, aby przechowywać razem jego dane, paragon i gwarancję.
archived|Zarchiwizowany
back|Wstecz
onboarding_privacy|Podstawowe funkcje działają offline. Zdjęcia i dokumenty pozostają w prywatnej pamięci aplikacji. O uprawnienia prosimy tylko podczas korzystania z danej funkcji.
warranty_start|Początek gwarancji
retailer|Sprzedawca
no_attachments|Brak załączników.
scan_receipt|Sprawdź paragon i odczytaj dane
duplicate|Duplikuj
delete_item|Usunąć ten przedmiot?
review_receipt|Sprawdź paragon
receipt_preview|Podgląd paragonu
ocr_notice|Tekst paragonu jest odczytywany na tym urządzeniu w wybranym języku. Wybierz stronę PDF, obróć lub przytnij obraz i sprawdź każdą sugestię. Oryginalne załączniki zostają zachowane.
extract_text|Odczytaj tekst
extracted_text|Odczytany tekst
cost|Zapisany koszt konserwacji
mark_complete|Oznacz jako wykonane
reports_title|Twój dom, udokumentowany.
include_prices|Uwzględnij zapisane wartości
export_complete|Raport został zapisany.
backup_password|Hasło (co najmniej 12 znaków)
premium|Possessory Premium
delete_all_body|Trwale usunąć wszystkie gospodarstwa domowe, przedmioty, załączniki i ustawienia z tego urządzenia? Wyeksportowane kopie zapasowe nie zostaną usunięte.
review_command|Sprawdź polecenie
all_conditions|Wszystkie stany
unlock_notice|Użyj kodu PIN, wzoru lub hasła blokady ekranu urządzenia, aby otworzyć Possessory.
app_lock_notice|Wymaga kodu PIN, wzoru lub hasła urządzenia. Possessory nie przechowuje tych danych uwierzytelniających. Blokada ukrywa także zrzuty ekranu i podglądy ostatnio używanych aplikacji.
edit_maintenance|Edytuj konserwację
maintenance_date|Data konserwacji
reminder_offsets|Wyprzedzenie przypomnień w dniach, oddzielone przecinkami
custom_reminder_hint|Podaj liczbę dni przed końcem gwarancji, na przykład 30,14,7,0. Dodatkowe daty mają format YYYY-MM-DD. Ustawienia oszczędzania energii mogą opóźniać codzienne kontrole.
ocr_language|Język paragonu
voice_move_title|Potwierdź przeniesienie przedmiotu
voice_room_missing|Nie znaleziono pasującego pokoju. Wybierz pokój na ekranie.
review_mode_notice|WERSJA TESTOWA: włączone robocze tłumaczenia i narzędzia oceny. Nie przyznaje żadnego zakupu ani uprawnienia Premium. Nie publikować w Appstore.
''')
TR = entries(r'''
appliances|Ev aletleri
tools|El aletleri
art|Sanat eserleri
tagline|Sahip olduğunuz her şey. Özenle düzenlenmiş.
recorded_value|Kaydedilen satın alma değeri
recently_added|Son eklenenler
newest|Son eklenenler
empty_inventory_body|Ayrıntıları, makbuzu ve garantiyi bir arada tutmak için bir eşya ekleyin.
view_all|Tümünü görüntüle
item_count|%1$d eşya
search_hint|Eşyaları, markaları, seri numaralarını ve notları arayın
filter|Filtre
expiring_soon|Yakında süresi dolacak
warranty_title|Biraz iç huzuru.
warranty_subtitle|Garanti bilgilerinizi elinizin altında tutun.
onboarding_privacy|Temel özellikler çevrimdışı çalışır. Fotoğraflar ve belgeler uygulamanın özel depolamasında kalır. İzinler yalnızca bir özelliği kullandığınızda istenir.
purchase_price|Satın alma fiyatı
warranty_start|Garanti başlangıcı
status|Durum
decimal_hint|Seçilen bölgenin ondalık ayırıcısını kullanarak tutar girin. Para birimi simgesi veya binlik ayırıcı kullanmayın.
add_receipt|Makbuz ekle
add_photo|Fotoğraf ekle
take_photo|Fotoğraf çek
no_attachments|Henüz ek yok.
scan_receipt|Makbuzu incele ve bilgileri çıkar
review_receipt|Makbuzu incele
receipt_preview|Makbuz önizlemesi
ocr_notice|Makbuz metni bu cihazda seçtiğiniz dilde çıkarılır. Bir PDF sayfası seçin, inceleme görüntüsünü döndürün veya kırpın ve her öneriyi kontrol edin. Orijinal ekler korunur.
crop_edges|Kenarları kırp
extract_text|Metni çıkar
extracted_text|Çıkarılan metin
apply_details|İncelenen bilgileri uygula
add_maintenance|Bakım ekle
repeat_days|Tekrar aralığı, gün (0 = bir kez)
mark_complete|Tamamlandı olarak işaretle
include_serials|Seri numaralarını dahil et
include_prices|Kaydedilen değerleri dahil et
export_report|Raporu kaydet
export_complete|Raporunuz kaydedildi.
inventory_report|Ev eşyası envanter kaydı
report_disclaimer|Kullanıcı tarafından tutulan envanterdir; sigorta şirketince onaylanmış bir değerleme değildir.
settings_subtitle|Kendinizi evinizde hissedin.
backup_password|Şifre (en az 12 karakter)
backup_complete|Yedekleme işlemi tamamlandı.
restore_preview|Geri yüklemeden önce yedeği incele
restore|Geri yükle
premium_unavailable|Bu test sürümünde satın alma etkin değildir. Mağaza ve satın alma doğrulaması ayarlanmalıdır. Mevcut sınır bir ev ve 30 eşyadır; mevcut kayıtlarınız erişilebilir kalır.
delete_all|Tüm yerel verileri sil
delete_all_body|Bu cihazdaki tüm evler, eşyalar, ekler ve ayarlar kalıcı olarak silinsin mi? Dışa aktardığınız yedekler silinmez.
listening|Dinleniyor… durdurmak için dokunun
tap_to_speak|Konuşmak için dokunun
voice_command|Komutunuz
review_command|Komutu incele
voice_confirm|İstediğiniz ekran açılıyor.
operation_failed|Bu işlem tamamlanamadı
all_conditions|Tüm eşya durumları
purchased_after|Bu tarihte veya sonrasında satın alınan
purchased_before|Bu tarihte veya öncesinde satın alınan
range_currency_notice|Fiyat aralığı için tek bir para birimi seçin. Farklı para birimlerindeki tutarlar asla karşılaştırılmaz veya dönüştürülmez.
apply_filters|Filtreleri uygula
clear_filters|Filtreleri temizle
save_search|Aramayı kaydet
saved_searches|Kaydedilen aramalar
manage_searches|Kaydedilen aramaları yönet
edit_household|Evi düzenle
unlock_notice|Possessory uygulamasını açmak için cihazınızın ekran kilidi PIN’ini, desenini veya şifresini kullanın.
app_lock_notice|Cihaz PIN’i, deseni veya şifresi gerekir. Possessory bu kimlik bilgilerini saklamaz. Kilitleme, ekran görüntülerini ve son uygulama önizlemelerini de gizler.
add_document|Belge ekle
delete_maintenance|Bakım kaydı silinsin mi?
edit_maintenance|Bakımı düzenle
reminder_offsets|Hatırlatma için önceden bildirilecek gün sayıları, virgülle ayrılmış
custom_reminder_dates|Ek hatırlatma tarihleri, virgülle ayrılmış
custom_reminder_hint|Ön bildirim, bitişten önceki gün sayısıdır; örneğin 30,14,7,0. Ek tarihler YYYY-MM-DD biçimindedir. Cihazın güç ayarları günlük kontrolleri geciktirebilir.
reset_crop|İnceleme görüntüsünü sıfırla
ocr_page|PDF sayfası
ocr_language|Makbuz dili
include_photos|Eşya fotoğraflarını dahil et
include_receipt_references|Ek referanslarını dahil et
column_item|Eşya
column_warranty|Garanti bitişi
no_receipt_reference|Ek referansı yok
voice_move_title|Eşyanın taşınmasını onayla
voice_room_missing|Eşleşen oda bulunamadı. Ekrandan bir oda seçin.
review_mode_notice|DEĞERLENDİRME SÜRÜMÜ: taslak çeviriler ve değerlendirme araçları etkindir. Satın alma veya Premium kullanım hakkı verilmez. Appstore’da yayımlanmaz.
warranty_extended|Uzatılmış garanti
''')

PL.update({
 'store:title':'Possessory – Domowy inwentarz i gwarancje',
 'store:iap_title':'Possessory Premium',
 'store:description':'Miejsce na wszystko, co posiadasz. Uporządkuj domowe przedmioty według pokojów, dodając zdjęcia, paragony, dane zakupu i terminy gwarancji. Przeszukuj wpisy, śledź gwarancje i zapisuj kopie ZIP na urządzeniu. Podstawowy inwentarz działa offline bez konta. Bezpłatna wersja obejmuje jedno gospodarstwo domowe, do 30 przedmiotów i trzy załączniki na przedmiot. Opcjonalny jednorazowy zakup Premium zapewnia nieograniczoną liczbę przedmiotów i gospodarstw domowych, OCR paragonów, zaawansowane przypomnienia, konserwację, eksport PDF/CSV/JSON, zapisane wyszukiwania, rozszerzone polecenia głosowe i kopie chronione hasłem. Bez subskrypcji; Amazon wyświetla lokalną cenę. Weryfikacja zakupu wymaga internetu; podpisany dostęp offline działa do 90 dni przed odnowieniem. Rozpoznawanie mowy zależy od dostawcy urządzenia i może korzystać z usługi sieciowej. Sprawdź sugestie z paragonu przed ich zastosowaniem. Raporty to wpisy prowadzone przez użytkownika, nie wyceny certyfikowane przez ubezpieczyciela.',
 'store:iap_description':'Jednorazowy zakup zapewniający nieograniczoną liczbę przedmiotów i gospodarstw domowych, OCR paragonów, zaawansowane przypomnienia gwarancyjne, konserwację, raporty PDF/CSV/JSON, zapisane wyszukiwania, rozszerzone polecenia głosowe i kopie chronione hasłem. Bez subskrypcji. Weryfikacja zakupu wymaga internetu.',
 'support:title':'Pomoc Possessory',
 'support:getting_started':'Utwórz gospodarstwo domowe, wybierz walutę i dodaj pokoje. Dodaj przedmiot z danymi zakupu i opcjonalnym terminem końca gwarancji. Konto nie jest wymagane.',
 'support:receipts':'Dołącz zdjęcie paragonu lub PDF, wybierz stronę i język paragonu, a następnie przytnij lub obróć obraz przed odczytaniem tekstu na urządzeniu. Sprawdź każdą sugestię przed jej zastosowaniem. Cały oryginalny załącznik zostaje zachowany. Zmiany waluty nigdy nie są ustalane bez poinformowania użytkownika.',
 'support:backup':'W Ustawieniach zapisz kopię ZIP z metadanymi i załącznikami. Możesz użyć hasła o długości co najmniej dwunastu znaków. Przechowuj je bezpiecznie, ponieważ nie można go odzyskać. Limit przywracania wynosi 100 MiB po rozpakowaniu; pojedynczy importowany załącznik może mieć do 25 MiB, a metadane do 8 MiB. Podczas przywracania załączniki są tymczasowo zapisywane w prywatnej pamięci podręcznej, aby ograniczyć zużycie pamięci operacyjnej.',
 'support:reminders':'Włącz opcjonalne przypomnienia w Ustawieniach i zezwól na powiadomienia. Possessory sprawdza nadchodzące terminy gwarancji i konserwacji mniej więcej codziennie. Zarządzanie energią urządzenia może opóźniać powiadomienia. Przypomnienia nie zastępują profesjonalnych kontroli bezpieczeństwa.',
 'support:voice':'Używaj poleceń pokazanych w aplikacji i sprawdzaj rozpoznany tekst. Obsługiwane działania obejmują nawigację, wyszukiwanie przedmiotów, liczenie przedmiotów w pokoju, tworzenie szkicu przedmiotu, odczytywanie przypomnień gwarancyjnych oraz przenoszenie jednoznacznie rozpoznanego przedmiotu po potwierdzeniu. Rozszerzone działania wymagają Premium. Wpisz polecenie, jeśli rozpoznawanie mowy jest niedostępne; dostawca tej usługi może korzystać z sieci.',
 'support:reports':'Wybierz pokój, kategorię lub filtr wartości dla określonej waluty oraz format wyjściowy. Przed zapisaniem sprawdź opcje numerów seryjnych, zapisanych wartości, zdjęć i odwołań do dokumentów. Przejrzyj podsumowanie podzielone na strony lub otwórz podgląd PDF. Raporty są inwentarzem prowadzonym przez użytkownika, a nie wyceną certyfikowaną przez ubezpieczyciela.',
 'support:delete':'Usuń przedmiot na ekranie jego szczegółów, aby usunąć wpis, załączniki i historię konserwacji. Aby usunąć wszystkie lokalne wpisy i ustawienia, wybierz Usuń wszystkie dane lokalne w Ustawieniach i potwierdź. Pliki wyeksportowane w inne miejsce nie zostaną usunięte.',
 'support:contact_label':'Skontaktuj się z pomocą',
 'support:app_lock':'Ustaw kod PIN, wzór lub hasło urządzenia, a następnie włącz ochronę przez odblokowanie w Ustawieniach. Possessory używa systemowego ekranu uwierzytelniania i nie przechowuje tych danych. Blokada ukrywa podglądy ostatnio używanych aplikacji i zrzuty ekranu. Jest to odrębne od szyfrowania zapisanych danych.',
})
TR.update({
 'store:title':'Possessory – Ev Envanteri ve Garanti Takibi',
 'store:iap_title':'Possessory Premium',
 'store:description':'Sahip olduğunuz her şey için bir yer. Ev eşyalarını fotoğraf, makbuz, satın alma bilgileri ve garanti tarihleriyle odalara göre düzenleyin. Kayıtları arayın, garantileri takip edin ve cihazınıza ZIP yedekleri kaydedin. Temel envanter çevrimdışı çalışır ve hesap gerektirmez. Ücretsiz sürüm bir ev, en fazla 30 eşya ve eşya başına üç ek içerir. İsteğe bağlı tek seferlik Premium satın alımı sınırsız eşya ve ev, makbuz OCR, gelişmiş hatırlatmalar, bakım, PDF/CSV/JSON dışa aktarma, kaydedilen aramalar, genişletilmiş ses komutları ve şifreli yedekler sunar. Abonelik yoktur; Amazon yerel fiyatı gösterir. Satın alma doğrulaması internet gerektirir; imzalı çevrimdışı erişim yenilenmeden önce en fazla 90 gün sürer. Konuşma tanıma cihaz sağlayıcısına bağlıdır ve ağ hizmeti kullanabilir. Makbuz önerilerini uygulamadan önce kontrol edin. Raporlar kullanıcı tarafından tutulan envanter kayıtlarıdır; sigorta şirketince onaylanmış değerlemeler değildir.',
 'store:iap_description':'Sınırsız eşya ve ev, makbuz OCR, gelişmiş garanti hatırlatmaları, bakım, PDF/CSV/JSON raporları, kaydedilen aramalar, genişletilmiş ses komutları ve şifreli yedekler için tek seferlik yükseltme. Abonelik yoktur. Satın alma doğrulaması internet gerektirir.',
})

ID = entries(r'''
home_office|Ruang kerja
recorded_value|Nilai pembelian tercatat
empty_inventory_body|Tambahkan barang untuk menyimpan rincian, tanda terima, dan garansinya bersama-sama.
view_all|Lihat semua
expiring_soon|Segera berakhir
living_room|Ruang tamu
back|Kembali
warranty_start|Awal garansi
warranty_provider|Penyedia garansi
warranty_type|Jenis garansi
warranty_notes|Catatan garansi
estimated_value|Perkiraan nilai saat ini menurut Anda
receipts_documents|Foto, tanda terima, dan dokumen
add_receipt|Lampirkan tanda terima
scan_receipt|Tinjau tanda terima dan ekstrak data
review_receipt|Tinjau tanda terima
receipt_preview|Pratinjau tanda terima
ocr_notice|Teks tanda terima diekstrak pada perangkat ini dalam bahasa yang Anda pilih. Pilih halaman PDF, putar atau pangkas gambar, dan periksa setiap saran. Lampiran asli tetap disimpan.
crop_edges|Pangkas tepi
mark_complete|Tandai selesai
cost|Biaya perawatan tercatat
export_complete|Laporan Anda telah disimpan.
inventory_report|Catatan inventaris rumah tangga
encrypted_backup|Cadangan yang dilindungi kata sandi
delete_all_body|Hapus secara permanen semua rumah tangga, barang, lampiran, dan pengaturan pada perangkat ini? Cadangan yang telah Anda ekspor tidak dihapus.
review_command|Tinjau perintah
range_currency_notice|Pilih satu mata uang untuk rentang harga. Jumlah dalam mata uang yang berbeda tidak pernah dibandingkan atau dikonversi.
household_currency_notice|Mata uang rumah tangga digunakan untuk catatan baru. Mata uang dan nilai barang yang sudah ada tetap dipertahankan.
unlock_notice|Gunakan PIN, pola, atau kata sandi kunci layar perangkat untuk membuka Possessory.
app_lock_notice|Memerlukan PIN, pola, atau kata sandi perangkat. Possessory tidak menyimpan kredensial tersebut. Penguncian juga menyembunyikan tangkapan layar dan pratinjau aplikasi terbaru.
maintenance_date|Tanggal perawatan
reminder_offsets|Jumlah hari sebelum berakhir untuk pengingat, dipisahkan koma
custom_reminder_hint|Waktu pengingat dihitung dalam hari sebelum berakhir, misalnya 30,14,7,0. Tanggal tambahan menggunakan YYYY-MM-DD. Pengaturan daya perangkat dapat menunda pemeriksaan harian.
ocr_language|Bahasa tanda terima
column_warranty|Tanggal berakhir garansi
warranty_manufacturer|Produsen
warranty_extended|Garansi tambahan
''')
ID['privacy_body']=r'''Inventaris, catatan rumah tangga, foto ruangan, tanda terima, dokumen perawatan, dan preferensi disimpan di penyimpanan pribadi aplikasi. Tidak diperlukan akun, iklan, atau analitik. Tidak ada sinkronisasi diam-diam. OCR tanda terima berjalan secara lokal dalam bahasa yang dipilih.\n\nMasukan suara bersifat opsional dan dapat menggunakan layanan jaringan dari penyedia pengenalan suara perangkat. Possessory tidak menyimpan rekaman mentah. Foto, tanda terima, catatan, dan nomor seri tidak dikirim ke layanan verifikasi pembelian.\n\nJika pembelian telah dikonfigurasi, ID pengguna Amazon dan ID tanda terima pembelian dikirim melalui HTTPS ke verifikator Possessory di Cloudflare Workers untuk memeriksa pembelian dan memulihkan akses. Verifikator memakai alamat IP koneksi dan hash ID pembelian untuk membatasi jumlah permintaan dalam waktu singkat. Tidak ada basis data pelanggan dan pencatatan permintaan aplikasi dinonaktifkan. Cloudflare memproses metadata jaringan untuk menjalankan dan mengamankan layanannya. Amazon menangani pembayaran secara terpisah. Akses offline terverifikasi dapat berlaku hingga 90 hari, atau sampai pembatalan terverifikasi yang lebih awal berlaku, sebelum perlu diperbarui secara online.\n\nEkspor dan cadangan hanya disimpan ke tujuan yang Anda pilih, yang mungkin berupa penyedia dokumen cloud. Ekspor biasa dan cadangan ZIP tidak dienkripsi. Cadangan dengan kata sandi menggunakan AES-GCM; kata sandi tidak dapat dipulihkan. Perlindungan buka kunci perangkat bersifat opsional dan tidak mengenkripsi basis data atau lampiran secara terpisah.\n\nHapus catatan tertentu atau semua data lokal melalui Pengaturan. Menghapus instalasi aplikasi menghapus catatan pribadi; salinan yang diekspor tetap di lokasi penyimpanannya. Simpan dan periksa cadangan sebelum menghapus instalasi. Hubungi dukungan untuk meminta penghapusan korespondensi privasi atau dukungan.'''
JA = {'premium':'Possessory Premium', 'all_conditions':'すべての状態', 'listening':'聞き取り中…タップして停止', 'operation_failed':'操作を完了できませんでした'}

HI = entries(r'''
home_office|घर का कार्यालय
tools|औज़ार
sort|क्रम बदलें
reminder_days|समाप्ति से पहले याद दिलाएँ (दिन)
archive|संग्रहित करें
delete|हटाएँ
rotate|घुमाएँ
apply_details|जाँचे गए विवरण लागू करें
next_due|अगली निर्धारित तारीख
cost|दर्ज रखरखाव खर्च
premium|Possessory Premium
preferences|प्राथमिकताएँ
clear_filters|फ़िल्टर साफ़ करें
purchased_after|इस तारीख को या इसके बाद खरीदा गया
purchased_before|इस तारीख को या इससे पहले खरीदा गया
app_locked|Possessory लॉक है
voice_unavailable|वाक् पहचान या कमांड उपलब्ध नहीं है। नीचे दिए गए समर्थित कमांड में से एक लिखें।
review_mode_notice|मूल्यांकन संस्करण: अनुवाद के मसौदे और मूल्यांकन उपकरण चालू हैं। कोई खरीद या Premium उपयोग-अधिकार नहीं मिलता। Appstore पर प्रकाशित न करें।
''')
AR = entries(r'''
format|الصيغة
delete_maintenance|هل تريد حذف سجل الصيانة؟
no_receipt_reference|لا توجد مراجع للمرفقات
language_system|الترجمات التجريبية مضمّنة وتعمل دون إنترنت. هي مسودات مولّدة آليًا تنتظر مراجعة اللغة والتخطيط. لا تُترجم أسماء مقتنياتك أو ملاحظاتك التي تدخلها بنفسك.
''')

ID.update({
 'store:title':'Possessory – Inventaris Rumah dan Garansi',
 'store:description':'Tempat untuk semua barang Anda. Atur barang rumah tangga menurut ruangan, lengkap dengan foto, tanda terima, rincian pembelian, dan tanggal garansi. Cari catatan, pantau garansi, dan simpan cadangan ZIP pada perangkat. Inventaris dasar bekerja offline tanpa akun. Versi gratis mencakup satu rumah tangga, hingga 30 barang dan tiga lampiran per barang. Pembelian Premium sekali bayar yang opsional menambahkan barang dan rumah tangga tanpa batas, OCR tanda terima, pengingat lanjutan, perawatan, ekspor PDF/CSV/JSON, pencarian tersimpan, perintah suara tambahan, dan cadangan berkata sandi. Tidak ada langganan; Amazon menampilkan harga lokal. Verifikasi pembelian memerlukan internet; akses offline bertanda tangan berlaku hingga 90 hari sebelum diperbarui. Pengenalan suara bergantung pada penyedia perangkat dan dapat memakai layanan jaringan. Periksa saran tanda terima sebelum menerapkannya. Laporan adalah catatan inventaris yang dikelola pengguna, bukan penilaian bersertifikat dari perusahaan asuransi.',
 'store:iap_description':'Peningkatan sekali bayar untuk barang dan rumah tangga tanpa batas, OCR tanda terima, pengingat garansi lanjutan, perawatan, laporan PDF/CSV/JSON, pencarian tersimpan, perintah suara tambahan, dan cadangan berkata sandi. Tanpa langganan. Verifikasi pembelian memerlukan internet.',
 'support:title':'Dukungan Possessory',
 'support:getting_started':'Buat rumah tangga, pilih mata uangnya, lalu tambahkan ruangan. Tambahkan barang dengan rincian pembelian dan tanggal berakhir garansi jika ada. Tidak diperlukan akun.',
 'support:where_is_my_data':'Catatan inventaris dan lampiran disimpan di penyimpanan pribadi aplikasi pada perangkat ini. Tidak ada sinkronisasi diam-diam. Menghapus instalasi akan menghapus data lokal, jadi simpan dan periksa cadangan terlebih dahulu.',
 'support:receipts':'Lampirkan gambar tanda terima atau PDF, pilih halaman dan bahasa tanda terima, lalu pangkas atau putar gambar sebelum mengekstrak teks pada perangkat. Periksa setiap saran sebelum menerapkannya. Lampiran asli lengkap tetap disimpan. Perubahan mata uang tidak pernah disimpulkan tanpa memberi tahu Anda.',
 'support:backup':'Di Pengaturan, simpan cadangan ZIP dengan metadata dan lampiran. Anda dapat memilih perlindungan kata sandi dengan setidaknya dua belas karakter. Simpan kata sandi dengan aman karena tidak dapat dipulihkan. Batas pemulihan adalah 100 MiB setelah dekompresi; setiap lampiran yang diimpor dibatasi 25 MiB dan metadata cadangan 8 MiB. Lampiran disimpan sementara di cache pribadi selama pemulihan untuk membatasi penggunaan memori.',
 'support:restore':'Buka cadangan di Pengaturan, masukkan kata sandi jika dienkripsi, lalu periksa pratinjau impor. Possessory memeriksa struktur, hash lampiran, dan referensi catatan. ID catatan yang ada tetap dipertahankan dan hanya catatan baru yang digabungkan. Periksa barang dan lampiran setelah pemulihan.',
 'support:voice':'Gunakan frasa perintah yang ditampilkan dalam aplikasi dan periksa teks yang dikenali. Tindakan yang didukung meliputi navigasi, pencarian barang, penghitungan barang per ruangan, pembuatan draf barang, pembacaan pengingat garansi, dan pemindahan barang yang dikenali secara unik setelah konfirmasi. Tindakan tambahan memerlukan Premium. Ketik perintah jika pengenalan suara tidak tersedia; penyedia suara dapat memakai layanan jaringan.',
 'support:reports':'Pilih ruangan, kategori, atau filter nilai untuk mata uang tertentu dan format keluaran. Periksa pengaturan nomor seri, nilai tercatat, foto, dan referensi dokumen sebelum menyimpan. Telusuri ringkasan berhalaman atau buka pratinjau PDF. Laporan adalah inventaris yang dikelola pengguna, bukan penilaian bersertifikat dari perusahaan asuransi.',
 'support:premium':'Premium adalah pembelian sekali bayar melalui Amazon tanpa langganan. Toko menampilkan harga lokal. Pembelian dan pemulihan memerlukan internet. ID pengguna Amazon dan tanda terima pembelian dikirim melalui HTTPS ke verifikator Possessory di Cloudflare Workers, yang memeriksa Amazon dan mengembalikan hak akses bertanda tangan. Inventaris rumah tangga dan foto tanda terima tidak dikirim. Verifikator memakai alamat IP koneksi dan hash ID pembelian untuk membatasi laju permintaan sementara, tidak memiliki basis data pelanggan, dan menonaktifkan pencatatan permintaan aplikasi. Cloudflare memproses metadata jaringan untuk menjalankan dan mengamankan layanannya. Akses offline bertanda tangan berlaku hingga 90 hari, atau sampai pembatalan terverifikasi yang lebih awal berlaku, sebelum perlu diperbarui. Catatan yang ada, penyuntingan biasa, penghapusan, dan pemulihan cadangan tetap tersedia tanpa Premium.',
 'support:warranties':'Tanggal dan ketentuan garansi dimasukkan oleh Anda. Garansi produsen dan hak konsumen menurut hukum adalah hal terpisah. Possessory tidak menentukan kelayakan atau memberikan nasihat hukum yang pasti.',
 'support:delete':'Hapus barang dari layar rinciannya untuk menghapus catatan, lampiran, dan riwayat perawatan. Untuk menghapus semua catatan dan pengaturan lokal, pilih Hapus semua data lokal di Pengaturan dan konfirmasikan. Berkas yang diekspor ke tempat lain tidak dihapus.',
 'support:contact_label':'Hubungi dukungan',
 'support:app_lock':'Atur PIN, pola, atau kata sandi perangkat, lalu aktifkan perlindungan buka kunci di Pengaturan. Possessory memakai layar kredensial sistem dan tidak menyimpan kredensial tersebut. Penguncian menyembunyikan pratinjau aplikasi terbaru dan tangkapan layar. Ini terpisah dari enkripsi data yang disimpan.',
})
HI.update({
 'store:iap_title':'Possessory Premium',
 'support:getting_started':'एक घर बनाएँ, उसकी मुद्रा चुनें और कमरे जोड़ें। सामान के खरीद विवरण और वैकल्पिक वारंटी समाप्ति तारीख दर्ज करें। खाता आवश्यक नहीं है।',
 'support:voice':'ऐप में दिखाई गई कमांड लिखें या बोलें और पहचाने गए पाठ की जाँच करें। समर्थित कार्यों में स्क्रीन खोलना, सामान खोजना, कमरे के सामान की गिनती, सामान का मसौदा बनाना, वारंटी रिमाइंडर पढ़ना और पुष्टि के बाद स्पष्ट रूप से पहचाने गए सामान को दूसरे कमरे में ले जाना शामिल है। अतिरिक्त कार्यों के लिए Premium चाहिए। वाक् पहचान उपलब्ध न होने पर कमांड लिखें; वाक् सेवा नेटवर्क इस्तेमाल कर सकती है।',
 'support:reports':'कमरा, श्रेणी या किसी एक मुद्रा का मूल्य फ़िल्टर और निर्यात प्रारूप चुनें। सहेजने से पहले सीरियल नंबर, दर्ज मूल्य, फोटो और दस्तावेज़ संदर्भ के विकल्प जाँचें। पृष्ठों में बँटा सारांश देखें या PDF पूर्वावलोकन खोलें। रिपोर्ट उपयोगकर्ता द्वारा रखे गए सामान के रिकॉर्ड हैं, बीमा कंपनी से प्रमाणित मूल्यांकन नहीं।',
 'support:delete':'सामान के विवरण वाली स्क्रीन से उसे हटाने पर उसका रिकॉर्ड, संलग्नक और रखरखाव इतिहास हट जाता है। सभी स्थानीय रिकॉर्ड और सेटिंग हटाने के लिए सेटिंग में सभी स्थानीय डेटा हटाएँ चुनें और पुष्टि करें। दूसरी जगह निर्यात की गई फ़ाइलें नहीं हटतीं।',
 'support:contact_label':'सहायता से संपर्क करें',
})
JA['store:iap_title']='Possessory Premium'

def main():
    path=ROOT/'localization/editorial-overrides.json'
    data=json.loads(path.read_text(encoding='utf-8'))
    for locale, corrections in [('es-ES',ES),('es-419',ES),('fr-FR',FR),('fr-CA',FR),('de-DE',DE),('it-IT',IT),('pt-PT',PTPT),('pt-BR',PTBR),('nl-NL',NL),('sv-SE',SV),('pl-PL',PL),('tr-TR',TR),('id-ID',ID),('ja-JP',JA),('hi-IN',HI),('ar',AR)]:
        data.setdefault(locale,{}).update(corrections)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Recorded catalogue corrections; no release approvals changed.')

if __name__=='__main__':main()
