{
    'name': 'DriveNow - Danos',
    'version': '19.0.1.3.0',
    'summary': 'DriveNow: danos, tarifário e revisões por km da frota',
    'category': 'Human Resources/Fleet',
    'depends': ['fleet'],
    'data': [
        'views/danos_views.xml',
        'views/tarifario_views.xml',
        'data/fleet.service.type.csv',
        'data/revisao_data.xml',
        'data/revisao_inicial.xml',
        'views/revisoes_views.xml',
    ],
    'installable': True,
    'license': 'LGPL-3',
}
