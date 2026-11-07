{
    'name': 'DriveNow - Danos',
    'version': '19.0.1.1.0',
    'summary': 'DriveNow: tabela de custos de danos, registo de danos e tarifário da frota',
    'category': 'Human Resources/Fleet',
    'depends': ['fleet'],
    'data': [
        'views/danos_views.xml',
        'views/tarifario_views.xml',
        'data/fleet.service.type.csv',
    ],
    'installable': True,
    'license': 'LGPL-3',
}
